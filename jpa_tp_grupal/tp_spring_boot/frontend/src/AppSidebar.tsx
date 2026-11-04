import type { MouseEvent } from "react";
import type { ArcaConfiguracionDTO } from "./models/factura.dto";

export type InvoiceSection = "pending" | "issued" | "clients" | "articles" | "brands" | "categories";

interface AppSidebarProps {
  active: InvoiceSection;
  arcaConfiguration: ArcaConfiguracionDTO | null;
  arcaConfigurationError: boolean;
  onChange: (section: InvoiceSection) => void;
  onInfo: () => void;
  onNewInvoice: () => void;
}

export function AppSidebar({
  active,
  arcaConfiguration,
  arcaConfigurationError,
  onChange,
  onInfo,
  onNewInvoice,
}: AppSidebarProps) {
  const navigate = (event: MouseEvent<HTMLAnchorElement>, section: InvoiceSection) => {
    event.preventDefault();
    onChange(section);
  };

  return (
    <aside className="app-sidebar" aria-label="Secciones de facturación">
      <header className="sidebar-header">
        <span className="sidebar-title">Mis facturas</span>
      </header>
      <nav className="sidebar-navigation">
        <span className="sidebar-group-label">Facturación</span>
        <a href="/sin-emitir" data-active={active === "pending"} onClick={(event) => navigate(event, "pending")}>
          <i className={`${active === "pending" ? "ph-fill" : "ph"} ph-clock`} aria-hidden="true" /><span>Sin emitir</span>
        </a>
        <a href="/emitidas" data-active={active === "issued"} onClick={(event) => navigate(event, "issued")}>
          <i className={`${active === "issued" ? "ph-fill" : "ph"} ph-seal-check`} aria-hidden="true" /><span>Emitidas</span>
        </a>
        <span className="sidebar-group-label">Operación</span>
        <a href="/clientes" data-active={active === "clients"} onClick={(event) => navigate(event, "clients")}>
          <i className={`${active === "clients" ? "ph-fill" : "ph"} ph-users`} aria-hidden="true" /><span>Clientes</span>
        </a>
        <a href="/articulos" data-active={active === "articles"} onClick={(event) => navigate(event, "articles")}>
          <i className={`${active === "articles" ? "ph-fill" : "ph"} ph-package`} aria-hidden="true" /><span>Artículos</span>
        </a>
        <div className="sidebar-submenu" aria-label="Administración de artículos">
          <a href="/articulos/marcas" data-active={active === "brands"} onClick={(event) => navigate(event, "brands")}>
            <i className={`${active === "brands" ? "ph-fill" : "ph"} ph-tag`} aria-hidden="true" /><span>Marcas</span>
          </a>
          <a href="/articulos/rubros" data-active={active === "categories"} onClick={(event) => navigate(event, "categories")}>
            <i className={`${active === "categories" ? "ph-fill" : "ph"} ph-squares-four`} aria-hidden="true" /><span>Rubros</span>
          </a>
        </div>
      </nav>
      <footer className="sidebar-footer">
        <button type="button" className="sidebar-info" onClick={onInfo}>
          <i className="ph ph-info" aria-hidden="true" /><span>Info</span>
        </button>
        <button type="button" className="sidebar-new-invoice" onClick={onNewInvoice}>
          <i className="ph ph-plus" aria-hidden="true" /><span>Nueva factura</span>
        </button>
        <span
          className="sidebar-environment"
          data-status={arcaConfiguration?.listaParaProbar
            ? "ready"
            : arcaConfigurationError
              ? "error"
              : arcaConfiguration
                ? "incomplete"
                : "loading"}
          tabIndex={0}
        >
          <i className="ph ph-flask" aria-hidden="true" /><span>Homologación</span>
          <span className="sidebar-environment-tooltip" role="tooltip">
            {arcaConfiguration ? (
              <>
                <strong>{arcaConfiguration.listaParaProbar ? "Configuración lista" : "Configuración incompleta"}</strong>
                <span><i data-ok={arcaConfiguration.cuitConfigurado} />CUIT: {arcaConfiguration.cuitConfigurado ? "cargado" : "sin cargar"}</span>
                <span><i data-ok={arcaConfiguration.keystoreExiste} />PKCS#12: {arcaConfiguration.keystoreExiste ? "encontrado" : "no encontrado"}</span>
                <span><i data-ok={arcaConfiguration.passwordConfigurada} />Contraseña: {arcaConfiguration.passwordConfigurada ? "cargada" : "sin cargar"}</span>
                <span><i data-ok={arcaConfiguration.endpointsHomologacion} />Endpoints: {arcaConfiguration.endpointsHomologacion ? "homologación" : "incorrectos"}</span>
              </>
            ) : arcaConfigurationError ? (
              <><strong>Sin conexión</strong><span>No se pudo consultar la configuración del backend.</span></>
            ) : (
              <><strong>Comprobando configuración</strong><span>Esperando la respuesta del backend.</span></>
            )}
          </span>
        </span>
      </footer>
    </aside>
  );
}
