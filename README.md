# myhomebuys

Plataforma personal de gestión del hogar: tickets de compra, despensa, listas de la compra, análisis de gasto.

## Stack

| Capa | Tecnología |
|------|-----------|
| Backend | Spring Boot 4.0.6, Java 21 |
| Templates | Thymeleaf 3.1 |
| Frontend | HTMX 2.0 + Tailwind CSS |
| BD | SQLite (embebida) |
| PDF parsing | Apache PDFBox 3.0.3 |

## Arranque

```bash
mvn spring-boot:run
# → http://localhost:8080/myhomebuys/
```

---

## Guía de estilo — Web Frontend

### Principios

- **Oscuro por defecto**: el tema base es oscuro, con toggle a claro vía localStorage
- **Sin build step**: todo se sirve desde Spring Boot, las dependencias frontend van por CDN
- **HTMX para interactividad**: navegación sin recarga, fragmentos Thymeleaf, cero JS de framework
- **Responsive**: experiencia completa en desktop, adaptada en móvil (menú hamburger)

### Dependencias CDN

| Recurso | URL | Notas |
|---------|-----|-------|
| Tailwind CSS | `cdn.tailwindcss.com` | Solo desarrollo. En producción usar CLI o PostCSS |
| HTMX | `cdn.jsdelivr.net/npm/htmx.org@2.0.10/dist/htmx.min.js` | |
| Chart.js | `cdn.jsdelivr.net/npm/chart.js` | Solo en dashboard |
| Inter (font) | `fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700` | Variable font |

### Paleta de color

Tema oscuro (`class="dark"` en `<html>`) con toggle claro/oscuro.

| Elemento | Claro | Oscuro |
|----------|-------|--------|
| Fondo página | `bg-gray-50` | `dark:bg-gray-950` |
| Fondo cards | `bg-white` | `dark:bg-gray-900` |
| Bordes | `border-gray-200` | `dark:border-gray-800` |
| Texto principal | `text-gray-900` | `dark:text-gray-100` |
| Texto secundario | `text-gray-600` | `dark:text-gray-400` |
| Texto muted | `text-gray-500` | `dark:text-gray-400` |
| Inputs / selects | `bg-white border-gray-300` | `dark:bg-gray-800 dark:border-gray-600 dark:text-gray-100` |
| Navbar | `bg-white border-gray-200` | `dark:bg-gray-900 dark:border-gray-800` |
| Tabla header | `bg-gray-50` | `dark:bg-gray-800/50` |
| Tabla row hover | `hover:bg-gray-50` | `dark:hover:bg-gray-800/50` |
| Dividers tabla | `divide-gray-100` | `dark:divide-gray-800` |

**Color de acento**: `indigo-600` / `indigo-700` (hover). No cambia entre temas.

- Botón primario: `bg-indigo-600 hover:bg-indigo-700 text-white`
- Icono activo en menú: `bg-indigo-50 text-indigo-700` (claro) / `dark:bg-indigo-900/30 dark:text-indigo-300`
- Links: `text-indigo-600 dark:text-indigo-400`

### Tipografía

- **Familia**: Inter (Google Fonts), fallback a `system-ui, sans-serif`
- **Pesos usados**: 400 (regular), 500 (medium), 600 (semibold), 700 (bold)
- **Escala** (Tailwind default):
  - `text-xs` → labels, metadata
  - `text-sm` → cuerpo, botones, tabla
  - `text-base` → valores destacados
  - `text-lg` → títulos de card
  - `text-xl` / `text-2xl` → headings de página
- **Mono**: `font-mono` para importes monetarios (€)

### Iconos

**Heroicons Outline v2.1.1** como fragmentos Thymeleaf inline en `templates/fragments/icons.html`.

Los iconos usan `stroke-width: 1.5` y `stroke: currentColor`. Heredan el color del padre vía Tailwind.

Uso en templates:
```html
<span th:insert="~{fragments/icons :: trash}" class="w-5 h-5"></span>
<span th:insert="~{fragments/icons :: home}" class="w-4 h-4"></span>
```

Iconos disponibles: `home`, `receipts`, `sun`, `moon`, `upload`, `trash`, `eye`, `search`, `chart`, `currency`, `calendar`, `back`, `close`, `menu`, `receipt`, `check`, `bars3`.

Para añadir uno nuevo:
1. Ve a [heroicons.com](https://heroicons.com)
2. Copia el SVG del icono outline
3. Añade un `<th:block th:fragment="nombre">` con el SVG en `icons.html`
4. Las clases del SVG deben ser `w-5 h-5` (el tamaño se sobreescribe desde el `th:insert`)

### Componentes

#### Layout (`layout.html`)
- `<html class="dark">` por defecto
- Script de inicialización del tema: lee `localStorage.theme`, fallback a `prefers-color-scheme`
- `body` con `transition-colors duration-200` para cambio suave de tema
- Navbar sticky + main centrado (`max-w-6xl`)
- Toast de éxito: verde esmeralda, auto-dismiss 4s, animación `slide-up`
- Modal de confirmación: overlay `bg-black/50 backdrop-blur-sm`, card centrada, botones Cancel/Delete

#### Menú (`fragments/menu.html`)
- Desktop: logo + links horizontales + toggle tema
- Móvil: hamburger (`#menu-toggle`) → despliega `#mobile-menu` vertical
- Módulo activo: highlight índigo (`bg-indigo-50 dark:bg-indigo-900/30`)
- Toggle tema: botón con icono sol/luna, cambia `dark` en `<html>`, persiste en `localStorage`

#### Modal de confirmación
- Función global `appConfirm(message, type)` → devuelve `Promise<boolean>`
- Tipos: `'danger'` (rojo, botón Delete) o cualquier otro (índigo, botón Confirm)
- Cierra con Cancel, botón Delete/Confirm, o clic fuera del modal
- Ejemplos de uso:
  ```html
  <!-- Form submit -->
  onclick="event.preventDefault(); appConfirm('Delete all receipts?', 'danger').then(ok => { if(ok) this.closest('form').submit(); })"
  
  <!-- HTMX delete con htmx.ajax() -->
  onclick="event.stopPropagation(); var btn=this; appConfirm('Delete this receipt?', 'danger').then(function(ok) { if(ok) htmx.ajax('DELETE', btn.dataset.deleteUrl, {target: btn.closest('tr'), swap: 'outerHTML'}); })"
  ```

#### Dashboard (home)
- 3 stat cards con icono en círculo de color (índigo, esmeralda, ámbar)
- Gráfico de barras Chart.js con datos mensuales (inyectados vía `th:inline="javascript"`)
- Lista de últimas 5 compras con icono, store, fecha, total y link a detalle

### Animaciones CSS

Definidas en `<style>` del layout:

| Clase | Efecto | Uso |
|-------|--------|-----|
| `animate-fade-in` | fade-in + slide-up 6px, 0.35s | Contenido principal de cada página |
| `animate-slide-up` | slide-up 12px, 0.3s | Toast de error |
| `animate-modal-in` | scale 0.95→1 + fade, 0.15s | Modal de confirmación |
| `toast-success` | slide-up 0.3s | Toast de éxito tras upload |
| `stat-card` | hover: translateY(-2px) + shadow | Cards del dashboard |
| `htmx-swapping` | opacity 0, 0.3s | Elementos saliendo vía HTMX |
| `htmx-indicator` | opacity toggle, 0.2s | Spinners de carga |

### Convenciones de templates

- **Layout pattern**: cada página usa `th:replace="~{layout :: layout}"` y define `<th:block th:fragment="content">`
- **Fragmentos HTMX**: devuelven solo el fragmento (ej: `receipts/fragments :: table`), no la página completa
- **Nombres de vista**: `module/action.html` (ej: `receipts/detail.html`)
- **Fragmentos compartidos**: `fragments/nombre.html`
- **Iconos**: `fragments/icons.html`
- **URLs Thymeleaf**: siempre con `@{}` (el context-path `/myhomebuys` se añade automáticamente)
- **Atributos HTMX**: usar prefijo `th:` cuando la URL es dinámica (ej: `th:hx-delete="@{/receipts/{id}(id=${r.id})}"`)

### Responsive

- **Breakpoint principal**: `md:` (768px)
- Desktop: menú horizontal, tabla completa, filtros en grid de 5 columnas
- Móvil: menú hamburger desplegable, tabla con scroll horizontal (`overflow-x-auto`), filtros en 2 columnas
- Stats cards: 1 columna en móvil, 3 en desktop
