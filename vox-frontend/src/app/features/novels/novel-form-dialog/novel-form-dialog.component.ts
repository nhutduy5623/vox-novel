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
import {
  CreateNovelRequest,
  NOVEL_STATUS_OPTIONS,
  NovelResponse,
  NovelStatus,
} from '../../../core/models/novel.model';
import { NovelService } from '../../../core/services/novel.service';
import { ApiError } from '../../../core/models/api-response.model';

@Component({
  selector: 'app-novel-form-dialog',
  imports: [ReactiveFormsModule],
  templateUrl: './novel-form-dialog.component.html',
  styleUrl: './novel-form-dialog.component.css',
})
export class NovelFormDialogComponent implements OnChanges {
  private readonly fb = inject(FormBuilder);
  private readonly novelService = inject(NovelService);

  @Input() open = false;
  @Input() novel: NovelResponse | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<NovelResponse>();

  readonly statusOptions = NOVEL_STATUS_OPTIONS;
  saving = false;
  errorMessage: string | null = null;

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    coverImageUrl: [''],
    status: ['DRAFT' as NovelStatus, Validators.required],
  });

  get isEdit(): boolean {
    return this.novel != null;
  }

  get dialogTitle(): string {
    return this.isEdit ? 'Sửa truyện' : 'Thêm truyện mới';
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open']?.currentValue || changes['novel']) {
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

    const payload: CreateNovelRequest = {
      title: this.form.controls.title.value.trim(),
      description: this.form.controls.description.value.trim() || undefined,
      coverImageUrl: this.form.controls.coverImageUrl.value.trim() || undefined,
      status: this.form.controls.status.value,
    };

    this.saving = true;
    this.errorMessage = null;

    const request$ = this.isEdit
      ? this.novelService.update(this.novel!.id, payload)
      : this.novelService.create(payload);

    request$.subscribe({
      next: (saved) => {
        this.saving = false;
        this.saved.emit(saved);
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage =
          err instanceof ApiError ? err.message : 'Không thể lưu truyện. Vui lòng thử lại.';
      },
    });
  }

  private patchForm(): void {
    if (this.novel) {
      this.form.reset({
        title: this.novel.title,
        description: this.novel.description ?? '',
        coverImageUrl: this.novel.coverImageUrl ?? '',
        status: this.novel.status,
      });
    } else {
      this.form.reset({
        title: '',
        description: '',
        coverImageUrl: '',
        status: 'DRAFT',
      });
    }
    this.errorMessage = null;
  }
}
