# Shop Platform Context

E-commerce course project providing customer-facing storefront display, shopping checkout, and administrative catalog management.

## Language

### Users & Access
**Customer**:
An end user who browses products, manages a personal profile, adds items to cart, and places orders.
_Avoid_: User (when ambiguous), Client, Buyer.

**Administrator**:
A privileged operator responsible for managing brands, categories, products, orders, and system settings.
_Avoid_: Manager, Superuser, Staff.

### Catalog
**Brand**:
A distinct commercial manufacturer or trademark associated with products (e.g., Apple, Nike).
_Avoid_: Maker, Vendor, Company.

**Category**:
A hierarchical classification node used to group related products (e.g., Electronics > Phones).
_Avoid_: Tag, Group, Classification.

**Product**:
An item listed in the shop catalog available for display or purchase, holding unified pricing and inventory.
_Avoid_: Item, Good, Merchandise, SKU.

### Commerce
**Cart Item**:
A transient line item representing a quantity of a specific product reserved by a customer prior to purchase.
_Avoid_: Basket line, shopping record.

**Order**:
A binding commercial agreement generated upon customer checkout containing snapshots of purchased products, total amount, and delivery details.
_Avoid_: Transaction, Deal, Purchase bill.

**Stock**:
The real-time available quantity of a product in warehouse inventory decremented atomically upon order placement.
_Avoid_: Inventory count, Balance.
