---
title: Shopping Cart and Order Checkout Commerce Slice
slug: 0005-cart-and-order-checkout
blocked-by: [0004-product-and-showcase]
---

# 0005 - Shopping Cart and Order Checkout Commerce Slice

## Delivers
Complete end-to-end commerce loop engineered as a textbook **Deep Module** with atomic concurrency control:

1. **Backend (`com.shop.modules.commerce`)**:
   - **Deep `OrderCheckoutService` Interface**:
     - `checkout(Long userId, CheckoutCommand cmd): OrderCheckoutResult`
     - `pay(Long userId, String orderNo): void`
     - `listCustomerOrders(Long userId, PageQuery query): PageResult<OrderSummaryVO>`
     - `getOrderDetail(Long userId, Long orderId): OrderDetailVO`
     - `markShipped(Long orderId): void` (Admin)
   - **Hidden Implementation Behind `checkout()`**:
     1. Reads checked cart items for `userId`.
     2. Validates item availability and pricing.
     3. **Atomic Inventory Decrement**: Executes single-statement database decrement:
        `UPDATE shop_product SET stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity}`
        Rolls back `@Transactional` boundary if affected rows == 0 (zero overselling guaranteed).
     4. Creates immutable snapshot records in `shop_order_item` (freezing product title, image, and price at purchase time).
     5. Generates unique order number (`order_no`) and saves `shop_order`.
     6. Clears checked items from `shop_cart_item`.
   - **Deep `CartService` Interface**:
     - `getCart(Long userId): CartSummaryVO`
     - `addItem(Long userId, AddCartCommand cmd): void`
     - `updateItem(Long userId, Long cartItemId, int quantity, boolean checked): void`
     - `removeItem(Long userId, Long cartItemId): void`
     - `clearCart(Long userId): void`
   - **Endpoints**:
     - Cart: `GET /api/cart`, `POST /api/cart/items`, `PUT /api/cart/items/{id}`, `DELETE /api/cart/items/{id}`, `DELETE /api/cart`.
     - Order: `POST /api/orders/checkout`, `POST /api/orders/{orderNo}/pay`, `GET /api/orders/my`, `GET /api/orders/{id}`.
     - Admin: `GET /api/admin/orders`, `PATCH /api/admin/orders/{id}/ship`.

2. **Frontend (`frontend/src/views/` & stores)**:
   - **Deep `useCartStore`**:
     - Provides simple methods `addToCart()`, `updateQuantity()`, `toggleCheck()`, `checkout()`.
     - Encapsulates cart badge count, local caching, and synchronization with backend.
   - **Views**:
     - Storefront Cart Drawer / Page (`/cart`): Stepper quantity selector, item selection, subtotal calculation.
     - Checkout Modal & Simulated Pay: One-click simulated payment flow transition.
     - Customer Order History (`/orders`): Status badge timeline and snapshot item cards.
     - Admin Order Console (`/admin/orders`): Order dispatch and shipment fulfillment action.

3. **Code Simplification & Testability**:
   - Deletion test: If `OrderCheckoutService` is deleted, all transaction, concurrency, snapshot, and inventory logic scatter across multiple callers. Its depth earns its keep.
   - Avoid shallow orchestration: Controllers never coordinate cart and inventory; `checkout()` handles the entire lifecycle atomically.
   - **Interface as Test Surface**: Multi-threaded concurrency integration test invokes `orderCheckoutService.checkout()` simultaneously from 10 threads against a product with 1 unit in stock, verifying exactly 1 order succeeds and 9 fail with clean business exceptions.

## Verification
- Add items to cart from storefront product detail.
- Update quantity and trigger checkout -> order created, cart emptied, and inventory atomically decremented.
- Execute automated multi-threaded concurrency test: no negative stock or overselling occurs.
- Pay order -> order status transitions to `PAID`.
- Log in as admin -> view new order and click "Mark as Shipped".
