import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, unwrapApiResponse } from '../models/api-response.model';
import {
  ChapterResponse,
  CreateChapterRequest,
  ScriptLineDto,
  TriggerAiScriptRequest,
  UpdateChapterRequest,
} from '../models/chapter.model';

@Injectable({ providedIn: 'root' })
export class ChapterService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.coreContentApiUrl}/api/chapters`;

  getById(id: number): Observable<ChapterResponse> {
    return this.http
      .get<ApiResponse<ChapterResponse>>(`${this.baseUrl}/${id}`)
      .pipe(map(unwrapApiResponse));
  }

  create(request: CreateChapterRequest): Observable<ChapterResponse> {
    return this.http
      .post<ApiResponse<ChapterResponse>>(this.baseUrl, request)
      .pipe(map(unwrapApiResponse));
  }

  update(id: number, request: UpdateChapterRequest): Observable<ChapterResponse> {
    return this.http
      .put<ApiResponse<ChapterResponse>>(`${this.baseUrl}/${id}`, request)
      .pipe(map(unwrapApiResponse));
  }

  delete(id: number): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.baseUrl}/${id}`)
      .pipe(map(() => undefined));
  }

  updateCharacterIds(chapterId: number, characterIds: number[]): Observable<ChapterResponse> {
    return this.http
      .patch<ApiResponse<ChapterResponse>>(`${this.baseUrl}/${chapterId}/characters`, {
        characterIds,
      })
      .pipe(map(unwrapApiResponse));
  }

  updateScript(id: number, scriptData: ScriptLineDto[]): Observable<ChapterResponse> {
    return this.http
      .patch<ApiResponse<ChapterResponse>>(`${this.baseUrl}/${id}/script`, scriptData)
      .pipe(map(unwrapApiResponse));
  }

  generateScript(request: TriggerAiScriptRequest): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.baseUrl}/generate-script`, request)
      .pipe(map(() => undefined));
  }

  generateAudio(chapterId: number): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.baseUrl}/${chapterId}/generate-audio`, null)
      .pipe(map(() => undefined));
  }

  requestMergeChapterAudio(chapterId: number): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.baseUrl}/${chapterId}/merge-audio`, null)
      .pipe(map(() => undefined));
  }
}
