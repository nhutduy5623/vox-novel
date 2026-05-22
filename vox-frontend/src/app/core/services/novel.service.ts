import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, unwrapApiResponse } from '../models/api-response.model';
import {
  CreateNovelRequest,
  NovelDetailResponse,
  NovelPagedQuery,
  NovelResponse,
} from '../models/novel.model';
import { Page } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class NovelService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.coreContentApiUrl}/api/novels`;

  getPaged(query: NovelPagedQuery): Observable<Page<NovelResponse>> {
    let params = new HttpParams()
      .set('page', query.page)
      .set('size', query.size)
      .set('sort', query.sort);

    if (query.title?.trim()) {
      params = params.set('title', query.title.trim());
    }

    return this.http
      .get<ApiResponse<Page<NovelResponse>>>(`${this.baseUrl}/paged`, { params })
      .pipe(map(unwrapApiResponse));
  }

  getById(id: number): Observable<NovelResponse> {
    return this.http
      .get<ApiResponse<NovelResponse>>(`${this.baseUrl}/${id}`)
      .pipe(map(unwrapApiResponse));
  }

  getDetail(novelId: number): Observable<NovelDetailResponse> {
    return this.http
      .get<ApiResponse<NovelDetailResponse>>(`${this.baseUrl}/detail/${novelId}`)
      .pipe(map(unwrapApiResponse));
  }

  create(request: CreateNovelRequest): Observable<NovelResponse> {
    return this.http
      .post<ApiResponse<NovelResponse>>(this.baseUrl, request)
      .pipe(map(unwrapApiResponse));
  }

  update(id: number, request: CreateNovelRequest): Observable<NovelResponse> {
    return this.http
      .put<ApiResponse<NovelResponse>>(`${this.baseUrl}/${id}`, request)
      .pipe(map(unwrapApiResponse));
  }

  delete(id: number): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.baseUrl}/${id}`)
      .pipe(map(() => undefined));
  }
}
