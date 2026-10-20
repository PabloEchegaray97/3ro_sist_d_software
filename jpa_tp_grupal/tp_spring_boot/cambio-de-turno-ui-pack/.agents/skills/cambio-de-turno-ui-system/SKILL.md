---
name: cambio-de-turno-ui-system
description: >
  Design system guide for the Cambio de Turno frontend. Use before creating or editing
  admin UI, modals, forms, wizards, lists, dashboards, tabs, empty states, and shared
  components when the goal is to keep the same visual style as TengoTurno.
---

# Cambio de Turno UI System

Esta skill porta el lenguaje visual admin de TengoTurno a `cambio-de-turno` sin depender del repo original.

## Antes De Tocar UI

- Primero inspeccionar componentes locales equivalentes. Si ya existen `Button`, `Dialog`, `AdminModalShell`, buscadores, bottom sheets o layout admin, extenderlos en vez de inventar variantes paralelas.
- Si faltan primitivos, leer `references/component-contract.md` y crear componentes compartidos antes de duplicar clases en cada pantalla.
- Si faltan tokens CSS, adaptar `references/base-theme.css` al `globals.css` del proyecto.
- Usar variables CSS con `var(...)`; evitar `bg-white`, `text-gray-*`, `border-gray-*` y colores hardcodeados salvo excepciones tecnicas claras como el canvas blanco de un QR.
- El tono admin es operativo: denso, claro, escaneable y sobrio. No hacer landing pages, heroes, ilustraciones decorativas ni composiciones de marketing para herramientas internas.

## Tokens Base

Usar estos nombres de variables como contrato:

```txt
--text-primary
--text-secondary
--text-tertiary
--text
--muted
--primary
--primary-brand
--primary-foreground
--surface-card
--surface-0
--surface-1
--surface-2
--bg-primary
--border-default-color
--border-subtle-color
--ring-primary
--selection
--interactive-selected-text
--interactive-selected-dot
--danger
--danger-foreground
--button-secondary-bg
--button-secondary-hover-bg
--button-ghost-hover-bg
--state-success
--state-success-soft
--state-success-border
```

En Tailwind usar formato `text-[color:var(--text-primary)]`, `bg-[color:var(--surface-card)]`, etc.

## Botones

- Importar siempre el `Button` compartido del proyecto.
- Variants esperadas: `primary`, `secondary`, `ghost`, `danger`.
- Sizes esperados: `sm`, `md`, `lg`.
- Props esperadas: `leftIcon`, `rightIcon`, `loading`, `loadingText`, `iconOnly`.
- No crear `outline` como variant. Si se necesita borde, partir de `secondary` o `ghost` con overrides puntuales.
- Botones icon-only: dimensiones estables (`h-* w-* px-0 gap-0`) y `aria-label`.
- Usar iconos de la libreria instalada del proyecto; no dibujar SVG manual si existe icono equivalente.

## Modales

Para dialogs compactos usar `Dialog` + `AdminModalShell`.

Reglas:
- Header, titulo, descripcion, X y spacing salen del shell. No armar headers distintos para modales hermanos.
- Si la X alcanza para cerrar, no agregar footer con boton "Cerrar".
- No poner card dentro de card: evitar wrappers internos `rounded-3xl bg-surface-0 p-*` salvo que sea una lista, selector o panel real.
- Para QR, el canvas puede ser blanco por escaneo, pero no envolver todo el contenido en una card clara.
- Para mapa/iframe, dejar que el shell maneje el padding. No sumar wrapper `px-* py-*` que aleje el iframe de los bordes del modal.
- No animar modales por defecto. Si hay transiciones de pasos del wizard, animar el paso, no el shell.

Ejemplo de body correcto para mapa:

```tsx
<AdminModalShell title="Ubicacion" description={address} widthClassName="w-[min(720px,calc(100vw-32px))]">
  <div className="overflow-hidden rounded-[1.35rem] bg-[color:var(--surface-1)]">
    <iframe className="h-[min(58vh,420px)] min-h-[280px] w-full border-0" />
  </div>
</AdminModalShell>
```

## Resumenes Inline En Wizards

Cuando un wizard ya selecciono una entidad (cliente, profesional, servicio, sede), mostrarla como resumen integrado, no como mini-card.

- No usar `rounded-3xl bg-surface-0 p-4` para el resumen.
- No usar avatar si no aporta.
- No usar `uppercase`, `tracking-wider` ni labels tipo badge para titulos normales.
- Usar sentence case: `Cliente`, `Servicio`, `Sede`.
- Si hace falta separar del siguiente bloque, usar `border-b border-default pb-4`.

```tsx
<div className="flex flex-col gap-3 border-b border-default pb-4 md:flex-row md:items-start md:justify-between">
  <div className="min-w-0">
    <p className="text-sm font-medium text-[color:var(--text-secondary)]">Cliente</p>
    <p className="mt-0.5 truncate text-base font-semibold text-[color:var(--text-primary)]">
      {clientName}
    </p>
    {phone ? (
      <p className="mt-1 flex items-center gap-1.5 truncate text-sm text-[color:var(--text-secondary)]">
        <Phone size={14} className="shrink-0" />
        {phone}
      </p>
    ) : null}
  </div>
  <Button type="button" variant="ghost" size="sm" onClick={onChange}>
    Cambiar
  </Button>
</div>
```

## Listas Seleccionables

Para listas dentro de modales:

- El contenedor puede usar `rounded-3xl bg-[color:var(--surface-1)]/56 p-1.5`.
- Cada item usa `rounded-3xl p-3`.
- Activo: `bg-selection text-[color:var(--interactive-selected-text)]`.
- Inactivo: `bg-surface-card text-[color:var(--text-primary)] hover:bg-[color:var(--surface-0)]`.
- Indicador activo: punto `bg-[color:var(--interactive-selected-dot)]`.
- Textos con `truncate`, `min-w-0`, `break-words` cuando haya datos largos.

## Empty States

Los empty states admin son minimalistas:

- Centrar contenido en el espacio disponible.
- No agregar card, borde o fondo decorativo solo para enmarcar el estado.
- Usar como maximo un icono discreto y un titulo corto.
- Omitir descripcion y CTA si no aportan una accion concreta.
- Para resultados filtrados, conservar solo acciones utiles como `Limpiar filtros`.

```tsx
<div className="flex min-h-[280px] w-full flex-1 items-center justify-center px-4 py-8 text-center text-[color:var(--text-primary)]">
  <div className="flex max-w-[360px] flex-col items-center">
    <CalendarClock aria-hidden="true" size={40} className="mb-3 shrink-0 text-current" />
    <h3 className="text-base font-bold leading-tight text-current lg:text-lg">
      No tenes turnos para esta fecha
    </h3>
  </div>
</div>
```

## Tabs Sticky Admin

Cuando una superficie admin tenga tabs operativas (`Resumen`/`Movimientos`, `Detalles`/`Tabla`, etc.), usar tabs sticky dentro de la card/panel antes que un segmented control suelto.

- La barra de tabs vive dentro de la superficie, no entre KPIs y contenido.
- Mobile sticky: `top: var(--admin-mobile-sticky-top, 0px)` y `z-[150]`.
- Fondo exterior: `bg-[var(--surface-0)]`.
- Tab activa: `bg-surface-card rounded-t-3xl`.
- Mobile con `overflow-x-auto`.
- Si se reemplaza un segmented control visible, quitar filtros o botones duplicados de esa barra; mantener filtros internos utiles del panel.

## Cabeceras Visuales De Entidad

Para una cabecera con logo/avatar + nombre de entidad:

- Acciones operativas (`Ubicacion`, `QR`, `Copiar link`, etc.) van arriba a la derecha dentro del contenedor visual del nombre, no en la fila del titulo de seccion.
- Nombre interno o codigo auxiliar va inline a la derecha del nombre principal en desktop; en mobile puede ocultarse o compactarse.
- Selectores de color/identidad interna van debajo del nombre, no mezclados con las acciones.
- Antes de mover controles, escribir una checklist literal de posicion y revisar el JSX final contra esa checklist.

## Busqueda Admin

Usar un buscador compartido si existe. Si no:

- `h-10`, `rounded-full`, `border-default`, `bg-surface-card`.
- Placeholder en `--muted`.
- Boton de busqueda integrado a la derecha cuando corresponda.
- No convertir busqueda/filtros en cards sueltas dentro de la pagina.

## Mobile Admin

- Margen exterior lateral del contenido: `px-2` en mobile.
- Topnav y bottomnav deben alinearse con ese margen.
- Evitar doble padding inferior: si el `main` del shell reserva espacio para bottom nav, no repetir padding grande en wrappers internos.
- Si mobile debe scrollear como pagina completa, usar `overflow-visible` en wrappers y reservar scroll interno solo para paneles/listas que realmente lo necesitan.
- Headers sticky dentro de cards usan dos capas: exterior `bg-[var(--surface-0)]`, interior `bg-surface-card rounded-t-3xl`.

## Scroll Externo En Configuraciones

En paginas de configuracion/formulario con una sola card principal:

- El scroll debe ser del `main` exterior, no de un body embebido dentro de la card.
- La card crece con su contenido: `overflow-visible`.
- Evitar `md:h-full md:overflow-hidden`, `md:flex-1 md:overflow-y-auto` y `custom-scrollbar` dentro de la card.
- Excepcion: listados largos, tablas y paneles operativos que necesitan scroll interno.

## Chat Bubbles Tipo WhatsApp

- Reservar carril lateral para el piquito con grilla tipo `grid-cols-[0.5rem_minmax(0,1fr)_0.5rem]`.
- No resolver overflow con `overflow-x-hidden`; ajustar layout real.
- Mostrar piquito solo en el primer mensaje de una cadena del mismo lado.
- Cliente a la izquierda; bot/admin a la derecha.
- Usar `min-w-0`, `break-words`, `whitespace-pre-wrap` y metadata con wrap.
- Botones dentro del bubble: `max-w-full`, `!whitespace-normal`, `break-words`, altura automatica.

## Badges

Para badges de notificacion o contadores:

- Sin bordes ni rings.
- Fondo `bg-[color:var(--primary)]`.
- `rounded-full`.
- Texto blanco y bold.

## Tipografia

| Uso | Clases |
|---|---|
| Titulo modal | `text-xl font-semibold text-[color:var(--text-primary)]` |
| Seccion heading | `text-lg font-bold leading-tight text-[color:var(--text)]` |
| Body | `text-sm text-[color:var(--text-secondary)]` |
| Label | `text-sm font-semibold text-[color:var(--text)]` |
| Caption | `text-xs text-[color:var(--text-tertiary)]` |
| Badge/chip | `text-xs font-semibold` |

## Radios

| Elemento | Radio |
|---|---|
| Modal container | `rounded-3xl` |
| Cards repetidas reales | `rounded-3xl` |
| Botones | `rounded-full` |
| Inputs | `rounded-3xl` |
| Badges/pills | `rounded-full` |
| Error alerts | `rounded-xl` |
