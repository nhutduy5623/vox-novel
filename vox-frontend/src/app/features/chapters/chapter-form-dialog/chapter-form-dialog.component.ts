import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  inject,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiError } from '../../../core/models/api-response.model';
import {
  CHAPTER_STATUS_OPTIONS,
  ChapterResponse,
  ChapterStatus,
  CreateChapterRequest,
  UpdateChapterRequest,
} from '../../../core/models/chapter.model';
import { ChapterService } from '../../../core/services/chapter.service';

@Component({
  selector: 'app-chapter-form-dialog',
  imports: [ReactiveFormsModule],
  templateUrl: './chapter-form-dialog.component.html',
  styleUrl: './chapter-form-dialog.component.css',
})
export class ChapterFormDialogComponent implements OnChanges {
  private readonly fb = inject(FormBuilder);
  private readonly chapterService = inject(ChapterService);

  @Input() open = false;
  @Input() novelId!: number;
  @Input() chapter: ChapterResponse | null = null;
  @Input() suggestedChapterNumber = 1;

  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<ChapterResponse>();

  readonly statusOptions = CHAPTER_STATUS_OPTIONS;
  saving = false;
  errorMessage: string | null = null;

  readonly form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    chapterNumber: [1, [Validators.required, Validators.min(1)]],
    originalContent: ['', Validators.required],
    status: ['DRAFT' as ChapterStatus],
  });

  get isEdit(): boolean {
    return this.chapter != null;
  }

  get dialogTitle(): string {
    return this.isEdit ? 'Sửa chương' : 'Thêm chương mới';
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open']?.currentValue || changes['chapter'] || changes['suggestedChapterNumber']) {
      this.patchForm();
    }
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('dialog-backdrop')) {
      this.close();
    }
  }

  close(): void {
    if (!this.saving) {
      this.errorMessage = null;
      this.closed.emit();
    }
  }

  submit(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }

    const title = this.form.controls.title.value.trim();
    const chapterNumber = this.form.controls.chapterNumber.value;
    const originalContent = this.form.controls.originalContent.value.trim();

    this.saving = true;
    this.errorMessage = null;

    if (this.isEdit) {
      const payload: UpdateChapterRequest = {
        title,
        chapterNumber,
        originalContent,
        status: this.form.controls.status.value,
      };
      this.chapterService.update(this.chapter!.id, payload).subscribe({
        next: (saved) => this.onSuccess(saved),
        error: (err) => this.onError(err),
      });
    } else {
      const payload: CreateChapterRequest = {
        novelId: this.novelId,
        title,
        chapterNumber,
        originalContent,
      };
      this.chapterService.create(payload).subscribe({
        next: (saved) => this.onSuccess(saved),
        error: (err) => this.onError(err),
      });
    }
  }

  private onSuccess(saved: ChapterResponse): void {
    this.saving = false;
    this.saved.emit(saved);
  }

  private onError(err: unknown): void {
    this.saving = false;
    this.errorMessage =
      err instanceof ApiError ? err.message : 'Không thể lưu chương. Vui lòng thử lại.';
  }

  private patchForm(): void {
    if (this.chapter) {
      this.form.reset({
        title: this.chapter.title,
        chapterNumber: this.chapter.chapterNumber,
        originalContent: this.chapter.originalContent,
        status: this.chapter.status,
      });
    } else {
      this.form.reset({
        title: '',
        chapterNumber: this.suggestedChapterNumber,
        originalContent: '',
        status: 'DRAFT',
      });
      this.form.controls.status.disable();
    }
    if (this.isEdit) {
      this.form.controls.status.enable();
    }
    this.errorMessage = null;
  }
}
