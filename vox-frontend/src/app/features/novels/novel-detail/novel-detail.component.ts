import { DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiError } from '../../../core/models/api-response.model';
import { ChapterResponse, chapterStatusLabel } from '../../../core/models/chapter.model';
import { NovelCharacterResponse } from '../../../core/models/character.model';
import { VoiceResponse } from '../../../core/models/voice.model';
import { forkJoin } from 'rxjs';
import {
  NovelDetailChapterItem,
  NovelDetailResponse,
  NovelResponse,
  novelStatusLabel,
} from '../../../core/models/novel.model';
import { NovelService } from '../../../core/services/novel.service';
import { ChapterService } from '../../../core/services/chapter.service';
import { CharacterService } from '../../../core/services/character.service';
import { VoiceService } from '../../../core/services/voice.service';
import {
  characterVoicePreviewUrl,
  playAudioUrl,
  stopAudio,
} from '../../../core/utils/audio.util';
import { NovelFormDialogComponent } from '../novel-form-dialog/novel-form-dialog.component';
import { ChapterFormDialogComponent } from '../../chapters/chapter-form-dialog/chapter-form-dialog.component';
import { CharacterFormDialogComponent } from '../../chapters/character-form-dialog/character-form-dialog.component';
import { CharacterJsonDialogComponent } from '../../chapters/character-json-dialog/character-json-dialog.component';

@Component({
  selector: 'app-novel-detail',
  imports: [
    RouterLink,
    DatePipe,
    NovelFormDialogComponent,
    ChapterFormDialogComponent,
    CharacterFormDialogComponent,
    CharacterJsonDialogComponent,
  ],
  templateUrl: './novel-detail.component.html',
  styleUrl: './novel-detail.component.css',
})
export class NovelDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly novelService = inject(NovelService);
  private readonly chapterService = inject(ChapterService);
  private readonly characterService = inject(CharacterService);
  private readonly voiceService = inject(VoiceService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly statusLabel = novelStatusLabel;
  readonly chapterStatusLabel = chapterStatusLabel;

  novel: NovelDetailResponse | null = null;
  novelCharacters: NovelCharacterResponse[] = [];
  narratorCharacters: NovelCharacterResponse[] = [];
  voices: VoiceResponse[] = [];

  loading = false;
  errorMessage: string | null = null;
  novelFormOpen = false;
  chapterFormOpen = false;
  editingChapter: ChapterResponse | null = null;
  deletingChapterId: number | null = null;

  characterFormOpen = false;
  characterJsonOpen = false;
  editingCharacter: NovelCharacterResponse | null = null;
  deletingCharacterId: number | null = null;

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.voiceService.getAll().subscribe({
      next: (voices) => (this.voices = voices),
      error: () => {},
    });

    this.route.paramMap.subscribe((params) => {
      const id = Number(params.get('id'));
      if (!Number.isFinite(id)) {
        this.errorMessage = 'ID truyện không hợp lệ';
        return;
      }
      this.loadDetail(id);
    });
  }

  ngOnDestroy(): void {
    stopAudio();
  }

  get suggestedChapterNumber(): number {
    if (!this.novel?.chapters.length) {
      return 1;
    }
    const max = Math.max(...this.novel.chapters.map((c) => c.chapterNumber));
    return max + 1;
  }

  characterPreviewUrl(character: NovelCharacterResponse): string | null {
    return characterVoicePreviewUrl(character, this.voices);
  }

  playCharacterVoice(character: NovelCharacterResponse, event: MouseEvent): void {
    event.stopPropagation();
    const url = this.characterPreviewUrl(character);
    if (!url) {
      window.alert('Nhân vật chưa có giọng preview.');
      return;
    }
    playAudioUrl(url);
  }

  openCreateCharacter(): void {
    this.editingCharacter = null;
    this.characterFormOpen = true;
  }

  openEditCharacter(character: NovelCharacterResponse, event?: MouseEvent): void {
    event?.stopPropagation();
    this.editingCharacter = character;
    this.characterFormOpen = true;
  }

  openCharacterJsonImport(): void {
    this.characterJsonOpen = true;
  }

  onCharacterFormClosed(): void {
    this.characterFormOpen = false;
    this.editingCharacter = null;
  }

  onCharacterSaved(_: NovelCharacterResponse): void {
    this.characterFormOpen = false;
    this.editingCharacter = null;
    if (this.novel) {
      this.loadCharacters(this.novel.id);
    }
  }

  onCharacterJsonClosed(): void {
    this.characterJsonOpen = false;
  }

  onCharacterJsonImported(_: number[]): void {
    this.characterJsonOpen = false;
    if (this.novel) {
      this.loadCharacters(this.novel.id);
    }
  }

  confirmDeleteCharacter(character: NovelCharacterResponse): void {
    const ok = window.confirm(
      `Xóa nhân vật "${character.name}" khỏi truyện? Hành động không thể hoàn tác.`,
    );
    if (!ok) {
      return;
    }

    this.deletingCharacterId = character.id;
    this.characterService.delete(character.id).subscribe({
      next: () => {
        this.deletingCharacterId = null;
        if (this.novel) {
          this.loadDetail(this.novel.id);
        }
      },
      error: (err) => {
        this.deletingCharacterId = null;
        this.errorMessage =
          err instanceof ApiError ? err.message : 'Không thể xóa nhân vật.';
      },
    });
  }

  openEditNovel(): void {
    this.novelFormOpen = true;
  }

  onNovelFormClosed(): void {
    this.novelFormOpen = false;
  }

  onNovelFormSaved(_: NovelResponse): void {
    this.novelFormOpen = false;
    if (this.novel) {
      this.loadDetail(this.novel.id);
    }
  }

  openCreateChapter(): void {
    this.editingChapter = null;
    this.chapterFormOpen = true;
  }

  openEditChapter(chapter: NovelDetailChapterItem, event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.chapterService.getById(chapter.id).subscribe({
      next: (full) => {
        this.editingChapter = full;
        this.chapterFormOpen = true;
      },
      error: (err) => {
        this.errorMessage =
          err instanceof ApiError ? err.message : 'Không tải được thông tin chương.';
      },
    });
  }

  onChapterFormClosed(): void {
    this.chapterFormOpen = false;
    this.editingChapter = null;
  }

  onChapterSaved(): void {
    this.chapterFormOpen = false;
    this.editingChapter = null;
    if (this.novel) {
      this.loadDetail(this.novel.id);
    }
  }

  viewChapter(chapter: NovelDetailChapterItem): void {
    if (!this.novel) {
      return;
    }
    void this.router.navigate(['/admin/novels', this.novel.id, 'chapters', chapter.id]);
  }

  confirmDeleteChapter(chapter: NovelDetailChapterItem, event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    const ok = window.confirm(`Xóa chương ${chapter.chapterNumber}?`);
    if (!ok) {
      return;
    }

    this.deletingChapterId = chapter.id;
    this.chapterService.delete(chapter.id).subscribe({
      next: () => {
        this.deletingChapterId = null;
        if (this.novel) {
          this.loadDetail(this.novel.id);
        }
      },
      error: (err) => {
        this.deletingChapterId = null;
        this.errorMessage =
          err instanceof ApiError ? err.message : 'Không thể xóa chương.';
      },
    });
  }

  confirmDeleteNovel(): void {
    if (!this.novel) {
      return;
    }
    const ok = window.confirm(`Xóa truyện "${this.novel.title}"?`);
    if (!ok) {
      return;
    }

    this.novelService.delete(this.novel.id).subscribe({
      next: () => {
        void this.router.navigate(['/admin/novels']);
      },
      error: (err) => {
        this.errorMessage =
          err instanceof ApiError ? err.message : 'Không thể xóa truyện.';
      },
    });
  }

  private loadDetail(id: number): void {
    this.loading = true;
    this.errorMessage = null;
    this.novel = null;

    this.novelService.getDetail(id).subscribe({
      next: (detail) => {
        this.loading = false;
        this.novel = detail;
        this.loadCharacters(id);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage =
          err instanceof ApiError ? err.message : 'Không tải được chi tiết truyện.';
      },
    });
  }

  private loadCharacters(novelId: number): void {
    forkJoin({
      novel: this.characterService.getByNovel(novelId),
      narrators: this.characterService.getByNovel(0),
    }).subscribe({
      next: ({ novel, narrators }) => {
        this.novelCharacters = novel;
        this.narratorCharacters = narrators;
      },
      error: () => {},
    });
  }
}
