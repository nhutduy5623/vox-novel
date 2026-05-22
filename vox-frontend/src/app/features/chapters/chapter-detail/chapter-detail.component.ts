import { DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, filter, interval, switchMap, takeUntil, takeWhile } from 'rxjs';
import { ApiError } from '../../../core/models/api-response.model';
import {
  ChapterResponse,
  ScriptLineDto,
  chapterStatusLabel,
} from '../../../core/models/chapter.model';
import { NovelCharacterResponse } from '../../../core/models/character.model';
import { VoiceResponse } from '../../../core/models/voice.model';
import { ChapterService } from '../../../core/services/chapter.service';
import { VoiceService } from '../../../core/services/voice.service';
import { resequenceScriptLines, resolveCharacterName } from '../../../core/utils/script-line.util';
import {
  characterVoicePreviewUrl,
  playAudioUrl,
  playSequentialAudio,
  stopAudio,
  stopSequentialPlayback,
} from '../../../core/utils/audio.util';
import { AssignChapterCharactersDialogComponent } from '../assign-chapter-characters-dialog/assign-chapter-characters-dialog.component';
import { CharacterAiMenuComponent } from '../character-ai-menu/character-ai-menu.component';
import { CharacterFormDialogComponent } from '../character-form-dialog/character-form-dialog.component';
import { CharacterJsonDialogComponent } from '../character-json-dialog/character-json-dialog.component';
import { ScriptLineDialogComponent } from '../script-line-dialog/script-line-dialog.component';

type ContentTab = 'original' | 'script';

@Component({
  selector: 'app-chapter-detail',
  imports: [
    RouterLink,
    DatePipe,
    AssignChapterCharactersDialogComponent,
    CharacterAiMenuComponent,
    CharacterFormDialogComponent,
    CharacterJsonDialogComponent,
    ScriptLineDialogComponent,
  ],
  templateUrl: './chapter-detail.component.html',
  styleUrl: './chapter-detail.component.css',
})
export class ChapterDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly chapterService = inject(ChapterService);
  private readonly voiceService = inject(VoiceService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly destroy$ = new Subject<void>();
  private readonly pollStop$ = new Subject<void>();
  private readonly draftPollStop$ = new Subject<void>();
  private readonly mergePollStop$ = new Subject<void>();

  readonly statusLabel = chapterStatusLabel;
  readonly resolveCharacterName = resolveCharacterName;

  novelId = 0;
  chapterId = 0;
  chapter: ChapterResponse | null = null;
  scriptLines: ScriptLineDto[] = [];
  voices: VoiceResponse[] = [];

  loading = false;
  errorMessage: string | null = null;
  infoMessage: string | null = null;

  contentTab: ContentTab = 'original';
  generatingScript = false;
  generatingAudio = false;
  mergingAudio = false;
  playingDrafts = false;
  savingScript = false;

  characterFormOpen = false;
  characterJsonOpen = false;
  assignCharactersOpen = false;
  editingCharacter: NovelCharacterResponse | null = null;

  scriptLineFormOpen = false;
  editingScriptLine: ScriptLineDto | null = null;
  scriptLineInsertIndex: number | null = null;

  generateScriptDialogOpen = false;
  selectedCharacterIds = new Set<number>();

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.voiceService.getAll().subscribe({
      next: (voices) => (this.voices = voices),
      error: () => {},
    });

    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      this.novelId = Number(params.get('novelId'));
      this.chapterId = Number(params.get('chapterId'));
      if (!Number.isFinite(this.novelId) || !Number.isFinite(this.chapterId)) {
        this.errorMessage = 'Tham số URL không hợp lệ';
        return;
      }
      this.loadChapter();
    });
  }

  ngOnDestroy(): void {
    stopAudio();
    stopSequentialPlayback();
    this.stopPolling();
    this.stopDraftPolling();
    this.stopMergePolling();
    this.destroy$.next();
    this.destroy$.complete();
  }

  setTab(tab: ContentTab): void {
    this.contentTab = tab;
  }

  get characters(): NovelCharacterResponse[] {
    return this.chapter?.characters ?? [];
  }

  get chapterCharacterIds(): number[] {
    return this.characters.map((c) => c.id);
  }

  get hasChapterAudio(): boolean {
    return !!this.chapter?.audioUrl?.trim();
  }

  get hasDraftAudio(): boolean {
    return this.scriptLines.some((l) => !!l.audioDraftLink?.trim());
  }

  openAssignCharacters(): void {
    this.assignCharactersOpen = true;
  }

  onAssignCharactersClosed(): void {
    this.assignCharactersOpen = false;
  }

  onAssignCharactersDone(): void {
    this.assignCharactersOpen = false;
    this.loadChapter();
  }

  openCreateCharacter(): void {
    this.editingCharacter = null;
    this.characterFormOpen = true;
  }

  openCharacterJsonImport(): void {
    this.characterJsonOpen = true;
  }

  onCharacterJsonClosed(): void {
    this.characterJsonOpen = false;
  }

  onCharacterJsonImported(newIds: number[]): void {
    this.characterJsonOpen = false;
    if (newIds.length > 0) {
      this.appendCharactersToChapter(newIds);
    } else {
      this.loadChapter();
    }
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

  playChapterAudio(): void {
    const url = this.chapter?.audioUrl?.trim();
    if (!url) {
      window.alert('Chương chưa có file audio.');
      return;
    }
    playAudioUrl(url);
  }

  openEditCharacter(character: NovelCharacterResponse, event?: MouseEvent): void {
    event?.stopPropagation();
    this.editingCharacter = character;
    this.characterFormOpen = true;
  }

  onCharacterFormClosed(): void {
    this.characterFormOpen = false;
    this.editingCharacter = null;
  }

  onCharacterSaved(saved: NovelCharacterResponse): void {
    const wasCreate = this.editingCharacter == null;
    this.characterFormOpen = false;
    this.editingCharacter = null;
    if (wasCreate) {
      this.appendCharactersToChapter([saved.id]);
    } else {
      this.loadChapter();
    }
  }

  confirmUnassignCharacter(character: NovelCharacterResponse): void {
    const ok = window.confirm(`Gỡ nhân vật "${character.name}" khỏi chương này?`);
    if (!ok) {
      return;
    }
    const ids = this.chapterCharacterIds.filter((id) => id !== character.id);
    this.chapterService.updateCharacterIds(this.chapterId, ids).subscribe({
      next: (updated) => this.applyChapter(updated),
      error: (err) => {
        this.errorMessage =
          err instanceof ApiError ? err.message : 'Không gỡ được nhân vật khỏi chương.';
      },
    });
  }

  private appendCharactersToChapter(newIds: number[]): void {
    const merged = [...new Set([...this.chapterCharacterIds, ...newIds])];
    this.chapterService.updateCharacterIds(this.chapterId, merged).subscribe({
      next: (updated) => this.applyChapter(updated),
      error: (err) => {
        this.errorMessage =
          err instanceof ApiError ? err.message : 'Không gán nhân vật vào chương.';
      },
    });
  }

  openGenerateScriptDialog(): void {
    this.selectedCharacterIds = new Set(this.characters.map((c) => c.id));
    this.generateScriptDialogOpen = true;
  }

  closeGenerateScriptDialog(): void {
    this.generateScriptDialogOpen = false;
  }

  toggleCharacterForScript(id: number, checked: boolean): void {
    if (checked) {
      this.selectedCharacterIds.add(id);
    } else {
      this.selectedCharacterIds.delete(id);
    }
  }

  isCharacterSelected(id: number): boolean {
    return this.selectedCharacterIds.has(id);
  }

  submitGenerateScript(): void {
    const characterIds = [...this.selectedCharacterIds];
    if (characterIds.length === 0) {
      this.errorMessage = 'Chọn ít nhất một nhân vật để tạo kịch bản.';
      return;
    }

    if (this.scriptLines.length > 0) {
      const ok = window.confirm(
        'Đã có kịch bản phân vai. Tạo mới sẽ thay thế kịch bản hiện tại. Tiếp tục?'
      );
      if (!ok) return;
    }

    this.generateScriptDialogOpen = false;
    this.generatingScript = true;
    this.errorMessage = null;
    this.infoMessage = 'Đang tạo kịch bản phân vai... Vui lòng đợi.';

    this.chapterService
      .generateScript({ chapterId: this.chapterId, characterIds })
      .subscribe({
        next: () => {
          this.contentTab = 'script';
          this.startScriptPolling();
        },
        error: (err) => {
          this.generatingScript = false;
          this.infoMessage = null;
          this.errorMessage =
            err instanceof ApiError ? err.message : 'Không gửi được yêu cầu tạo kịch bản.';
        },
      });
  }

  generateAudio(): void {
    if (this.scriptLines.length === 0) {
      window.alert('Chưa có kịch bản. Vui lòng tạo kịch bản trước khi tạo audio nháp.');
      return;
    }

    if (this.hasDraftAudio) {
      const ok = window.confirm('Đã có audio nháp. Tạo lại sẽ ghi đè audio nháp hiện tại. Tiếp tục?');
      if (!ok) return;
    }

    this.generatingAudio = true;
    this.errorMessage = null;
    this.infoMessage = 'Đã đưa yêu cầu tạo audio nháp vào hàng đợi...';

    this.chapterService.generateAudio(this.chapterId).subscribe({
      next: () => {
        this.startDraftPolling(this.hasDraftAudio);
      },
      error: (err) => {
        this.generatingAudio = false;
        this.infoMessage = null;
        this.errorMessage =
          err instanceof ApiError ? err.message : 'Không thể tạo audio cho chương.';
      },
    });
  }

  playAllDrafts(): void {
    const draftLinks = this.scriptLines
      .filter((l) => l.audioDraftLink?.trim())
      .map((l) => l.audioDraftLink!.trim());

    if (draftLinks.length === 0) {
      window.alert('Chưa có audio nháp nào để phát.');
      return;
    }

    this.playingDrafts = true;
    playSequentialAudio(draftLinks, () => {
      this.playingDrafts = false;
    });
  }

  mergeChapterAudio(): void {
    if (this.scriptLines.length === 0) {
      window.alert('Chưa có kịch bản. Vui lòng tạo kịch bản trước khi tạo audio hoàn chỉnh.');
      return;
    }

    if (!this.hasDraftAudio) {
      window.alert('Chưa có audio nháp. Vui lòng tạo audio nháp trước khi tạo audio hoàn chỉnh.');
      return;
    }

    if (this.hasChapterAudio) {
      const ok = window.confirm('Chương đã có audio. Tạo lại audio hoàn chỉnh sẽ ghi đè. Tiếp tục?');
      if (!ok) return;
    }

    this.mergingAudio = true;
    this.errorMessage = null;
    this.infoMessage = 'Đã đưa yêu cầu ghép audio vào hàng đợi...';

    this.chapterService.requestMergeChapterAudio(this.chapterId).subscribe({
      next: () => {
        this.startMergePolling();
      },
      error: (err) => {
        this.mergingAudio = false;
        this.infoMessage = null;
        this.errorMessage =
          err instanceof ApiError ? err.message : 'Không thể gửi yêu cầu ghép audio.';
      },
    });
  }

  openAddScriptLine(): void {
    this.editingScriptLine = null;
    this.scriptLineInsertIndex = null;
    this.scriptLineFormOpen = true;
  }

  openEditScriptLine(line: ScriptLineDto): void {
    this.editingScriptLine = line;
    this.scriptLineInsertIndex = null;
    this.scriptLineFormOpen = true;
  }

  onScriptLineFormClosed(): void {
    this.scriptLineFormOpen = false;
    this.editingScriptLine = null;
    this.scriptLineInsertIndex = null;
  }

  onScriptLineSaved(line: ScriptLineDto): void {
    let next: ScriptLineDto[];
    if (this.editingScriptLine) {
      next = this.scriptLines.map((l) =>
        l.sequence === this.editingScriptLine!.sequence ? { ...line, sequence: l.sequence } : l,
      );
    } else if (this.scriptLineInsertIndex != null) {
      next = [...this.scriptLines];
      next.splice(this.scriptLineInsertIndex, 0, line);
    } else {
      next = [...this.scriptLines, line];
    }
    this.scriptLineFormOpen = false;
    this.editingScriptLine = null;
    this.scriptLineInsertIndex = null;
    this.persistScript(resequenceScriptLines(next));
  }

  deleteScriptLine(line: ScriptLineDto): void {
    const ok = window.confirm('Xóa dòng kịch bản này?');
    if (!ok) {
      return;
    }
    const next = this.scriptLines.filter((l) => l.sequence !== line.sequence);
    this.persistScript(resequenceScriptLines(next));
  }

  insertScriptLinePlaceholder(afterIndex: number): void {
    window.alert('Chèn đoạn thoại giữa hai dòng — API chưa sẵn sàng. Dùng "Thêm dòng" tạm thời.');
    void afterIndex;
  }

  regenerateLineAudioPlaceholder(): void {
    window.alert('Tạo lại audio cho dòng — API chưa sẵn sàng.');
  }

  playDraftAudio(link: string | null | undefined): void {
    if (!link?.trim()) {
      window.alert('Chưa có audio nháp cho dòng này.');
      return;
    }
    playAudioUrl(link.trim());
  }

  private persistScript(lines: ScriptLineDto[]): void {
    this.savingScript = true;
    this.chapterService.updateScript(this.chapterId, lines).subscribe({
      next: (updated) => {
        this.savingScript = false;
        this.applyChapter(updated);
      },
      error: (err) => {
        this.savingScript = false;
        this.errorMessage =
          err instanceof ApiError ? err.message : 'Không lưu được kịch bản.';
      },
    });
  }

  loadChapter(): void {
    this.loading = true;
    this.errorMessage = null;

    this.chapterService.getById(this.chapterId).subscribe({
      next: (chapter) => {
        this.loading = false;
        this.applyChapter(chapter);
        if (chapter.status === 'PROCESSING_AI' && this.generatingScript) {
          this.startScriptPolling();
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage =
          err instanceof ApiError ? err.message : 'Không tải được chi tiết chương.';
      },
    });
  }

  private applyChapter(chapter: ChapterResponse): void {
    this.chapter = chapter;
    this.scriptLines = [...(chapter.scriptData ?? [])].sort(
      (a, b) => a.sequence - b.sequence,
    );
    if (
      this.generatingScript &&
      chapter.status !== 'PROCESSING_AI' &&
      (this.scriptLines.length > 0 || chapter.status === 'FAILED')
    ) {
      this.finishScriptGeneration(chapter);
    }

    if (
      this.generatingAudio &&
      chapter.status !== 'PROCESSING_AI' &&
      (this.hasDraftAudio || chapter.status === 'FAILED')
    ) {
      this.finishDraftGeneration(chapter);
    }
  }

  private startScriptPolling(): void {
    this.stopPolling();

    interval(3000)
      .pipe(
        takeUntil(this.pollStop$),
        takeUntil(this.destroy$),
        takeWhile(() => this.generatingScript, true),
        switchMap(() => this.chapterService.getById(this.chapterId)),
        filter(() => this.generatingScript),
      )
      .subscribe({
        next: (chapter) => this.applyChapter(chapter),
        error: () => this.finishScriptGeneration(null, 'Mất kết nối khi chờ kịch bản.'),
      });

    setTimeout(() => {
      if (this.generatingScript) {
        this.finishScriptGeneration(null, 'Hết thời gian chờ. Thử tải lại trang.');
      }
    }, 120000);
  }

  private stopPolling(): void {
    this.pollStop$.next();
  }

  private startDraftPolling(hadDraftBefore: boolean): void {
    this.stopDraftPolling();

    interval(3000)
      .pipe(
        takeUntil(this.draftPollStop$),
        takeUntil(this.destroy$),
        takeWhile(() => this.generatingAudio, true),
        switchMap(() => this.chapterService.getById(this.chapterId)),
        filter(() => this.generatingAudio),
      )
      .subscribe({
        next: (chapter) => {
          this.applyChapter(chapter);
          if (!hadDraftBefore && this.hasDraftAudio && chapter.status !== 'PROCESSING_AI') {
            this.finishDraftGeneration(chapter);
          }
        },
        error: () => {
          this.finishDraftGeneration(null, 'Mất kết nối khi chờ tạo audio nháp.');
        },
      });

    setTimeout(() => {
      if (this.generatingAudio) {
        this.finishDraftGeneration(null, 'Hết thời gian chờ. Thử tải lại trang.');
      }
    }, 180000);
  }

  private stopDraftPolling(): void {
    this.draftPollStop$.next();
  }

  private startMergePolling(): void {
    this.stopMergePolling();

    interval(3000)
      .pipe(
        takeUntil(this.mergePollStop$),
        takeUntil(this.destroy$),
        takeWhile(() => this.mergingAudio, true),
        switchMap(() => this.chapterService.getById(this.chapterId)),
        filter(() => this.mergingAudio),
      )
      .subscribe({
        next: (chapter) => {
          this.applyChapter(chapter);
          if (chapter.audioUrl?.trim()) {
            this.stopMergePolling();
            this.mergingAudio = false;
            this.infoMessage = 'Đã ghép audio hoàn chỉnh thành công.';
          }
        },
        error: () => {
          this.stopMergePolling();
          this.mergingAudio = false;
          this.infoMessage = null;
          this.errorMessage = 'Mất kết nối khi chờ ghép audio.';
        },
      });

    setTimeout(() => {
      if (this.mergingAudio) {
        this.stopMergePolling();
        this.mergingAudio = false;
        this.infoMessage = 'Hết thời gian chờ. Thử tải lại trang.';
      }
    }, 180000);
  }

  private stopMergePolling(): void {
    this.mergePollStop$.next();
  }

  private finishDraftGeneration(chapter: ChapterResponse | null, timeoutMsg?: string): void {
    this.stopDraftPolling();
    this.generatingAudio = false;

    if (timeoutMsg) {
      this.infoMessage = timeoutMsg;
      return;
    }

    if (chapter?.status === 'FAILED') {
      this.infoMessage = null;
      this.errorMessage = 'Tạo audio nháp thất bại. Vui lòng thử lại.';
      return;
    }

    if (this.hasDraftAudio) {
      this.infoMessage = 'Đã tạo audio nháp thành công.';
      this.contentTab = 'script';
    } else {
      this.infoMessage = 'Yêu cầu đã gửi. Audio nháp có thể cần thêm thời gian — bấm Tải lại nếu cần.';
    }
  }

  private finishScriptGeneration(chapter: ChapterResponse | null, timeoutMsg?: string): void {
    this.stopPolling();
    this.generatingScript = false;

    if (timeoutMsg) {
      this.infoMessage = timeoutMsg;
      return;
    }

    if (chapter?.status === 'FAILED') {
      this.infoMessage = null;
      this.errorMessage = 'Tạo kịch bản thất bại. Vui lòng thử lại.';
      return;
    }

    if (this.scriptLines.length > 0) {
      this.infoMessage = 'Đã tạo kịch bản phân vai thành công.';
      this.contentTab = 'script';
    } else {
      this.infoMessage = 'Yêu cầu đã gửi. Kịch bản có thể cần thêm thời gian — bấm Tải lại nếu cần.';
    }
  }
}
