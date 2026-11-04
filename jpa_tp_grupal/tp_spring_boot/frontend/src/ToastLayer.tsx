import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { Toaster } from "sileo";

const currentLayer = (): HTMLElement => {
  const dialogs = Array.from(document.querySelectorAll<HTMLDialogElement>("dialog[open]"));
  return dialogs.at(-1) ?? document.body;
};

export function ToastLayer() {
  // El host conserva siempre la misma identidad. Solo cambia de padre para
  // quedar dentro del dialog activo y, por lo tanto, sobre su top layer.
  // Así Toaster no se desmonta y su temporizador no vuelve a empezar.
  const [host] = useState<HTMLDivElement>(() => {
    const element = document.createElement("div");
    element.dataset.toastLayerHost = "true";
    return element;
  });

  useEffect(() => {
    const syncLayer = () => {
      const layer = currentLayer();
      if (host.parentElement !== layer) layer.appendChild(host);
    };
    const observer = new MutationObserver(syncLayer);

    window.addEventListener("app-dialog-stack-change", syncLayer);
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ["open"],
    });
    syncLayer();
    return () => {
      observer.disconnect();
      window.removeEventListener("app-dialog-stack-change", syncLayer);
      host.remove();
    };
  }, [host]);

  return createPortal(<Toaster position="top-center" />, host);
}
