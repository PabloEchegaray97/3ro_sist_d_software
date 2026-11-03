import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { Toaster } from "sileo";

const currentLayer = (): HTMLElement => {
  const dialogs = Array.from(document.querySelectorAll<HTMLDialogElement>("dialog[open]"));
  return dialogs.at(-1) ?? document.body;
};

export function ToastLayer() {
  const [target, setTarget] = useState<HTMLElement>(() => currentLayer());

  useEffect(() => {
    const syncLayer = () => setTarget(currentLayer());
    window.addEventListener("app-dialog-stack-change", syncLayer);
    syncLayer();
    return () => window.removeEventListener("app-dialog-stack-change", syncLayer);
  }, []);

  return createPortal(<Toaster position="top-center" />, target);
}
