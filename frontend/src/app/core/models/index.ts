// ============================================================
//  共用 TypeScript 型別定義
//  Phase 1 後端 API 確定後再細化這些 interface
// ============================================================

export interface User {
  id: number;
  email: string;
  name: string;
  phone?: string;
  role: 'CUSTOMER' | 'ADMIN';
}

export interface Category {
  id: number;
  name: string;
  slug: string;
  parentId?: number;
  sortOrder: number;
}

export interface Product {
  id: number;
  categoryId: number;
  name: string;
  description?: string;
  price: number;
  stockQty: number;
  weightGrams?: number;
  isFrozen: boolean;
  isActive: boolean;
  images: ProductImage[];
}

export interface ProductImage {
  id: number;
  url: string;
  isPrimary: boolean;
  sortOrder: number;
}

export interface CartItem {
  productId: number;
  productName: string;
  price: number;
  quantity: number;
  imageUrl?: string;
}

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'PREPARING'
  | 'SHIPPED'
  | 'COMPLETED'
  | 'CANCELLED';

export interface Order {
  id: number;
  status: OrderStatus;
  totalAmount: number;
  shippingName: string;
  shippingPhone: string;
  shippingAddress: string;
  shippingNote?: string;
  paidAt?: string;
  createdAt: string;
  items: OrderItem[];
}

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
}

export interface ApiResponse<T> {
  data: T;
  message?: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
