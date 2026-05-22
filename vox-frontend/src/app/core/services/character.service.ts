import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, unwrapApiResponse } from '../models/api-response.model';
import {
  CreateCharacterRequest,
  NovelCharacterResponse,
} from '../models/character.model';

@Injectable({ providedIn: 'root' })
export class CharacterService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.coreContentApiUrl}/api/characters`;

  getByNovel(novelId: number): Observable<NovelCharacterResponse[]> {
    return this.http
      .get<ApiResponse<NovelCharacterResponse[]>>(`${this.baseUrl}/novel/${novelId}`)
      .pipe(map(unwrapApiResponse));
  }

  create(request: CreateCharacterRequest): Observable<NovelCharacterResponse> {
    return this.http
      .post<ApiResponse<NovelCharacterResponse>>(this.baseUrl, request)
      .pipe(map(unwrapApiResponse));
  }

  update(id: number, request: CreateCharacterRequest): Observable<NovelCharacterResponse> {
    return this.http
      .put<ApiResponse<NovelCharacterResponse>>(`${this.baseUrl}/${id}`, request)
      .pipe(map(unwrapApiResponse));
  }

  delete(id: number): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.baseUrl}/${id}`)
      .pipe(map(() => undefined));
  }
}
