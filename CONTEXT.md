# Shop Platform Context

E-commerce course project providing customer-facing storefront display, shopping checkout, and administrative catalog management.

## Language

### Surfaces
**Storefront**:
The customer-facing shop under `/` (home showcase, product search, product detail, cart, order history).
_Avoid_: Frontend (when referring to a business surface), Mall page.

**Admin Console**:
The privileged management surface under `/admin` for dashboard, users, brands, categories, products, and orders.
_Avoid_: Backend (when referring to a UI surface), Dashboard (as a synonym for the whole console).

### Users & Access
**Customer**:
An end user who browses products, manages a personal profile, adds items to cart, and places orders. Identified in commerce code as `customerId` while the physical database column remains `user_id`.
_Avoid_: User (when ambiguous), Client, Buyer.

**Administrator**:
A privileged operator responsible for managing brands, categories, products, orders, and user accounts.
_Avoid_: Manager, Superuser, Staff.

### Catalog
**Brand**:
A distinct commercial manufacturer or trademark associated with products (e.g., Apple, Nike).
_Avoid_: Maker, Vendor, Company.

**Brand Origin**:
The country or region where a brand originates (e.g., "美国加利福尼亚"), stored as `origin` and shown on both storefront and admin surfaces.
_Avoid_: Country, Factory location.

**Category**:
A hierarchical classification node used to group related products (e.g., Electronics > Phones); parents automatically include descendant categories in product filtering.
_Avoid_: Tag, Group, Classification.

**Product**:
An item listed in the shop catalog available for display or purchase, holding unified pricing and inventory.
_Avoid_: Item, Good, Merchandise, SKU.

**Main Image / Sub Images**:
The single required cover image used by lists, cart, and order snapshots; the optional gallery images used only on the product detail page.
_Avoid_: Thumbnail, Banner (when referring to product images).

**Showcase**:
The aggregated storefront home payload (carousel, featured brands, category tree, hot products, new products) served by one request and cached under `catalog:showcase:home`.
_Avoid_: Home page data, Feed.

### Commerce
**Cart Item**:
A transient line item representing a quantity of a specific product reserved by a customer prior to purchase, with its own checked flag and subtotal behavior.
_Avoid_: Basket line, shopping record.

**Checkout**:
The transactional act of turning checked cart items into an order: validate products, atomically decrement stock, freeze snapshots, clear settled cart items.
_Avoid_: Payment, Submit.

**Order**:
A binding commercial agreement generated upon customer checkout containing snapshots of purchased products, total amount, and delivery details.
_Avoid_: Transaction, Deal, Purchase bill.

**Order Item Snapshot**:
The immutable record of a purchased product's title, image, unit price, quantity, and subtotal frozen at checkout time, isolated from later catalog edits.
_Avoid_: Order detail line, Product copy.

**Order Status**:
The lifecycle state of an order: `UNPAID` → `PAID` → `SHIPPED` → `COMPLETED`, with `CANCELLED` reachable from `UNPAID` or `PAID` (only before shipment).
_Avoid_: Order state (informal), Progress.

**Stock**:
The real-time available quantity of a product in warehouse inventory, decremented atomically upon checkout and restored atomically when an unshipped order is cancelled.
_Avoid_: Inventory count, Balance.
