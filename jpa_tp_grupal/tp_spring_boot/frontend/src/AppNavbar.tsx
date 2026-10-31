interface AppNavbarProps {
  onNewInvoice: () => void;
}

export function AppNavbar({ onNewInvoice }: AppNavbarProps) {
  return (
    <nav className="app-navbar" aria-label="Navegación principal">
      <div className="navbar-content">
        <div className="navbar-brand">
          <span className="navbar-title">Mis facturas</span>
        </div>
        <button type="button" data-variant="primary" onClick={onNewInvoice}><i className="ph ph-plus" aria-hidden="true" /><span>Nueva factura</span></button>
      </div>
    </nav>
  );
}
