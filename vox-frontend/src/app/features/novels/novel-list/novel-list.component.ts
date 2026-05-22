import { DatePipe, isPlatformBrowser } from '@angular/common';
import { Component, OnDestroy, OnInit, PLATFORM_ID, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { ApiError } from '../../../core/models/api-response.model';
import { Page } from '../../../core/models/page.model';
import {
  NOVEL_SORT_OPTIONS,
  NovelResponse,
  novelStatusLabel,
} from '../../../core/models/novel.model';
import { NovelService } from '../../../core/services/novel.service';
import { NovelFormDialogComponent } from '../novel-form-dialog/novel-form-dialog.component';

@Component({
  selector: 'app-novel-list',
  imports: [FormsModule, DatePipe, NovelFormDialogComponent],
  templateUrl: './novel-list.component.html',
  styleUrl: './novel-list.component.css',
})
export class NovelListComponent implements OnInit, OnDestroy {
  private readonly novelService = inject(NovelService);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly destroy$ = new Subject<void>();
  private readonly search$ = new Subject<string>();

  readonly sortOptions = NOVEL_SORT_OPTIONS;
  readonly statusLabel = novelStatusLabel;
  readonly pageSizeOptions = [10, 20, 50];

  novels: NovelResponse[] = [];
  loading = false;
  errorMessage: string | null = null;

  searchTitle = '';
  sort = 'createdAt,desc';
  page = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  formOpen = false;
  editingNovel: NovelResponse | null = null;
  deletingId: number | null = null;

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.search$
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => {
        this.page = 0;
        this.loadNovels();
      });

    this.loadNovels();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearchInput(): void {
    this.search$.next(this.searchTitle);
  }

  onSortChange(): void {
    this.page = 0;
    this.loadNovels();
  }

  onPageSizeChange(): void {
    this.page = 0;
    this.loadNovels();
  }

  goToPage(target: number): void {
    if (target < 0 || target >= this.totalPages || target === this.page) {
      return;
    }
    this.page = target;
    this.loadNovels();
  }

  openCreate(): void {
    this.editingNovel = null;
    this.formOpen = true;
  }

  openEdit(novel: NovelResponse, event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.editingNovel = novel;
    this.formOpen = true;
  }

  onFormClosed(): void {
    this.formOpen = false;
    this.editingNovel = null;
  }

  onFormSaved(): void {
    this.formOpen = false;
    this.editingNovel = null;
    this.loadNovels();
  }

  confirmDelete(novel: NovelResponse, event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();

    const ok = window.confirm(`Xóa truyện "${novel.title}"? Hành động không thể hoàn tác.`);
    if (!ok) {
      return;
    }

    this.deletingId = novel.id;
    this.errorMessage = null;

    this.novelService.delete(novel.id).subscribe({
      next: () => {
        this.deletingId = null;
        if (this.novels.length === 1 && this.page > 0) {
          this.page -= 1;
        }
        this.loadNovels();
      },
      error: (err) => {
        this.deletingId = null;
        this.errorMessage =
          err instanceof ApiError ? err.message : 'Không thể xóa truyện. Vui lòng thử lại.';
      },
    });
  }

  viewDetail(novel: NovelResponse): void {
    void this.router.navigate(['/admin/novels', novel.id]);
  }

  loadNovels(): void {
    this.loading = true;
    this.errorMessage = null;

    this.novelService
      .getPaged({
        title: this.searchTitle,
        page: this.page,
        size: this.pageSize,
        sort: this.sort,
      })
      .subscribe({
        next: (page) => this.applyPage(page),
        error: (err) => {
          this.loading = false;
          this.errorMessage =
            err instanceof ApiError
              ? err.message
              : 'Không tải được danh sách truyện. Kiểm tra core-content-service (8082).';
        },
      });
  }

  private applyPage(page: Page<NovelResponse>): void {
    this.loading = false;
    this.novels = page.content;
    this.totalElements = page.totalElements;
    this.totalPages = page.totalPages;
    this.page = page.number;
  }
}
