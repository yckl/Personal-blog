export interface Article {
  id: number;
  title: string;
  slug: string;
  excerpt?: string;
  coverImage?: string;
  authorId?: number;
  categoryId?: number;
  status?: string;
  visibility?: string;
  isTop?: boolean;
  isFeatured?: boolean;
  allowComment?: boolean;
  viewCount?: number;
  likeCount?: number;
  commentCount?: number;
  wordCount?: number;
  publishedAt?: string;
  createdAt?: string;
  updatedAt?: string;
  // frontend specific attributes commonly appended
  content?: string;
}

export interface User {
  id: number;
  username: string;
  nickname: string;
  email?: string;
  avatar?: string;
  roles?: string[];
  permissions?: string[];
}
