export type NovelStatus = 'DRAFT' | 'ONGOING' | 'COMPLETED' | 'PAUSED';

export interface NovelResponse {
  id: number;
  title: string;
  description?: string;
  coverImageUrl?: string;
  status: NovelStatus;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateNovelRequest {
  title: string;
  description?: string;
  coverImageUrl?: string;
  status: NovelStatus;
}

export interface NovelDetailChapterItem {
  id: number;
  chapterNumber: number;
  status: string;
}

export interface NovelDetailCharacterItem {
  id: number;
  name: string;
  gender?: string;
  previewUrl?: string;
}

export interface NovelDetailResponse extends NovelResponse {
  chapters: NovelDetailChapterItem[];
  characters: NovelDetailCharacterItem[];
}

export interface NovelPagedQuery {
  title?: string;
  page: number;
  size: number;
  sort: string;
}

export const NOVEL_STATUS_OPTIONS: { value: NovelStatus; label: string }[] = [
  { value: 'DRAFT', label: 'Bản nháp' },
  { value: 'ONGOING', label: 'Đang ra' },
  { value: 'COMPLETED', label: 'Hoàn thành' },
  { value: 'PAUSED', label: 'Tạm ngưng' },
];

export const NOVEL_SORT_OPTIONS: { value: string; label: string }[] = [
  { value: 'createdAt,desc', label: 'Mới nhất' },
  { value: 'createdAt,asc', label: 'Cũ nhất' },
  { value: 'title,asc', label: 'Tên A → Z' },
  { value: 'title,desc', label: 'Tên Z → A' },
  { value: 'updatedAt,desc', label: 'Cập nhật gần đây' },
];

export function novelStatusLabel(status: NovelStatus): string {
  return NOVEL_STATUS_OPTIONS.find((o) => o.value === status)?.label ?? status;
}
