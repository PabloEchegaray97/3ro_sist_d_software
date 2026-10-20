# Component Contract

Usar esta referencia cuando el proyecto Cambio de Turno no tenga todavia los primitivos necesarios.

## Button

Debe existir un `Button` compartido con:

- Variants: `primary`, `secondary`, `ghost`, `danger`.
- Sizes: `sm`, `md`, `lg`.
- Props: `leftIcon`, `rightIcon`, `loading`, `loadingText`, `iconOnly`.
- Siempre `rounded-full`.
- Icon-only requiere `aria-label`.
- No agregar variant `outline`; si se necesita borde, partir de `secondary` o `ghost` con overrides puntuales.

Clases base recomendadas:

```tsx
"inline-flex relative shrink-0 select-none items-center justify-center whitespace-nowrap rounded-full border-0 font-semibold tracking-[0.01em] transition-[background-color,color,opacity,transform] duration-200 ease-out focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[color:var(--ring-primary)] focus-visible:ring-offset-2 focus-visible:ring-offset-[color:var(--bg-primary)] active:translate-y-[1px] disabled:pointer-events-none disabled:opacity-65"
```

## AdminModalShell

Debe envolver los dialogs admin compactos y proveer:

- `bg-surface-card`, `rounded-3xl`, `border-0`, `shadow-2xl`.
- Header con titulo, descripcion opcional y X transparente sin fondo.
- Body scrollable con padding propio.
- Footer opcional para acciones reales, no para duplicar un "Cerrar" si ya existe la X.

Estructura recomendada:

```tsx
<DialogContent
  className={cn(
    "!grid w-[min(460px,calc(100vw-32px))] max-w-none max-h-[calc(100dvh-32px)] grid-rows-[auto_minmax(0,1fr)_auto] gap-0 overflow-hidden rounded-3xl border-0 bg-surface-card p-0 text-[color:var(--text-primary)] shadow-2xl sm:max-h-[90vh] [&>button]:hidden",
    widthClassName,
    contentClassName,
  )}
>
  <div className="flex items-start gap-4 px-5 pt-5 sm:px-6 sm:pt-6">
    <DialogHeader className="min-w-0 flex-1 space-y-1.5 text-left">
      <DialogTitle className="text-xl font-semibold tracking-tight text-[color:var(--text-primary)]">
        {title}
      </DialogTitle>
      {description ? (
        <DialogDescription className="m-0 text-sm leading-relaxed text-[color:var(--text-secondary)]">
          {description}
        </DialogDescription>
      ) : null}
    </DialogHeader>
    <DialogClose asChild>
      <button className="inline-flex shrink-0 items-center justify-center bg-transparent p-0 text-[color:var(--text-primary)] hover:bg-transparent">
        <X size={20} strokeWidth={2.2} />
      </button>
    </DialogClose>
  </div>
  <div className="min-h-0 overflow-y-auto px-5 pt-4 pb-5 custom-scrollbar sm:px-6 sm:pt-5 sm:pb-6">
    {children}
  </div>
</DialogContent>
```

## Search Input

El buscador admin debe sentirse como control de trabajo, no marketing:

- Alto estable (`h-10`).
- `rounded-full`.
- Fondo `bg-surface-card`.
- Borde `border-default`.
- Boton de busqueda integrado a la derecha cuando corresponda.

## Bottom Sheet

En mobile, usar un bottom sheet para selectores secundarios y formularios auxiliares:

- Overlay `bg-black/45` o `bg-black/60`.
- Panel `rounded-t-3xl bg-surface-card`.
- Drag handle si el componente lo soporta.
- En desktop, preferir contenido inline o `AdminModalShell`.

## Inline Summaries

Para mostrar un objeto ya seleccionado dentro de un wizard:

- No crear mini-card.
- No usar uppercase ni tracking ancho.
- Usar `border-b border-default pb-4`, label normal y accion `Cambiar`.

## Sticky Tabs

Para tabs de paneles admin:

- Barra dentro de la card/lista.
- Exterior `bg-[var(--surface-0)]`.
- Tab activa `bg-surface-card rounded-t-3xl`.
- Mobile sticky con `top: var(--admin-mobile-sticky-top, 0px)` y `z-[150]`.
