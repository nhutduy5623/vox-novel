import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ApiError } from '../../../core/models/api-response.model';
import { NovelCharacterResponse } from '../../../core/models/character.model';
import { ChapterService } from '../../../core/services/chapter.service';
import { CharacterService } from '../../../core/services/character.service';

@Component({
  selector: 'app-assign-chapter-characters-dialog',
  imports: [FormsModule],
  templateUrl: './assign-chapter-characters-dialog.component.html',
  styleUrl: './assign-chapter-characters-dialog.component.css',
})
export class AssignChapterCharactersDialogComponent implements OnChanges {
  private readonly characterService = inject(CharacterService);
  private readonly chapterService = inject(ChapterService);

  @Input() open = false;
  @Input() novelId!: number;
  @Input() chapterId!: number;
  @Input() currentCharacterIds: number[] = [];

  @Output() closed = new EventEmitter<void>();
  @Output() assigned = new EventEmitter<void>();

  novelCharacters: NovelCharacterResponse[] = [];
  narratorCharacters: NovelCharacterResponse[] = [];
  searchName = '';
  selectedIds = new Set<number>();
  selectedNarratorId: number | null = null;
  loading = false;
  saving = false;
  errorMessage: string | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open']?.currentValue) {
      this.loadNovelCharacters();
      this.selectedIds = new Set();
      this.searchName = '';
      this.errorMessage = null;
    }
  }

  get availableCharacters(): NovelCharacterResponse[] {
    const current = new Set(this.currentCharacterIds);
    const keyword = this.searchName.trim().toLowerCase();
    return this.novelCharacters.filter((c) => {
      if (current.has(c.id)) {
        return false;
      }
      if (!keyword) {
        return true;
      }
      return c.name.toLowerCase().includes(keyword);
    });
  }

  get hasNarrators(): boolean {
    return this.narratorCharacters.length > 0;
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('dialog-backdrop')) {
      this.close();
    }
  }

  close(): void {
    if (!this.saving) {
      this.closed.emit();
    }
  }

  toggle(id: number, checked: boolean): void {
    if (checked) {
      this.selectedIds.add(id);
    } else {
      this.selectedIds.delete(id);
    }
  }

  isSelected(id: number): boolean {
    return this.selectedIds.has(id);
  }

  submit(): void {
    if (this.hasNarrators && this.selectedNarratorId == null) {
      this.errorMessage = 'Chọn 1 người dẫn truyện.';
      return;
    }
    const toAdd = [...this.selectedIds];
    if (toAdd.length === 0 && this.selectedNarratorId == null) {
      this.errorMessage = 'Chọn ít nhất một nhân vật.';
      return;
    }

    const narratorIds = new Set(this.narratorCharacters.map((c) => c.id));
    const withoutOldNarrators = this.currentCharacterIds.filter((id) => !narratorIds.has(id));
    const merged = [
      ...new Set([
        ...withoutOldNarrators,
        ...toAdd,
        ...(this.selectedNarratorId != null ? [this.selectedNarratorId] : []),
      ]),
    ];
    this.saving = true;
    this.errorMessage = null;

    this.chapterService.updateCharacterIds(this.chapterId, merged).subscribe({
      next: () => {
        this.saving = false;
        this.assigned.emit();
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage =
          err instanceof ApiError ? err.message : 'Không gán được nhân vật vào chương.';
      },
    });
  }

  private loadNovelCharacters(): void {
    this.loading = true;
    forkJoin({
      novel: this.characterService.getByNovel(this.novelId),
      narrators: this.characterService.getByNovel(0),
    }).subscribe({
      next: ({ novel, narrators }) => {
        this.loading = false;
        this.novelCharacters = novel;
        this.narratorCharacters = narrators;
        const narratorIds = new Set(narrators.map((c) => c.id));
        const currentNarrator = this.currentCharacterIds.find((id) => narratorIds.has(id)) ?? null;
        this.selectedNarratorId = currentNarrator;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage =
          err instanceof ApiError ? err.message : 'Không tải danh sách nhân vật.';
      },
    });
  }
}
