# Cambio de Turno UI Pack

Carpeta portable para llevar el estilo admin de TengoTurno a otro proyecto.

## Como usar

1. Copia la carpeta `.agents` de este pack a la raiz del proyecto `cambio-de-turno`.
2. En el otro proyecto invoca la skill como `$cambio-de-turno-ui-system` cuando trabajes en vistas, modales, formularios o componentes UI.
3. Si el proyecto todavia no tiene tokens CSS equivalentes, copia/adapta `.agents/skills/cambio-de-turno-ui-system/references/base-theme.css` dentro de su `globals.css`.
4. Si faltan primitivos compartidos, usa `.agents/skills/cambio-de-turno-ui-system/references/component-contract.md` como contrato para crear `Button`, `AdminModalShell`, busqueda, bottom sheets y patrones relacionados.

## Que trae

- Una skill portable: `.agents/skills/cambio-de-turno-ui-system/SKILL.md`.
- Tokens CSS base para reproducir superficies, texto, botones, seleccion, estados y foco.
- Contratos de componentes para recrear el mismo lenguaje visual sin depender del repo original.

## Que no trae

- Codigo acoplado a `tengo_turno_bot` o `tengo_turno_app`.
- Endpoints, modelos, rutas o reglas de negocio.
- Una libreria de iconos obligatoria. Usa la que tenga el proyecto, manteniendo tamanos y jerarquia visual.
