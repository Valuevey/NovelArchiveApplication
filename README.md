# 📚 소설 개인 보관함 프로그램 (Novel Bookshelf)

## 📖 프로젝트 소개

Java Swing을 이용하여 제작한 '개인용 소설 텍스트 파일 통합 관리 시스템'입니다.

사용자는 로컬 하드디스크에 보유 중인 소설 텍스트 회차 파일들을 프로그램에 연동하고, 로컬 텍스트 파일을 편리하게 열람할 수 있습니다.

---


## 📂 프로젝트 구조
src
├── Main.java               # 프로그램 시작점 및 메인 서재 로딩 제어
├── BookShelfPage.java      # 메인 보관함 UI, 작품 카드 목록 렌더링 및 메타데이터 IO
├── NovelDetailPage.java    # 소설 정보 상세창, 회차 스캔, 책갈피 탭 및 토글 정렬 엔진
├── AddNovelDialog.java     # 새 소설 추가/수정 창, 가변 확장형 플랫폼 팝업 로직
├── AppSettings.java        # 커스텀 플랫폼 등록 및 환경설정 관리 데이터 스토리지
├── Novel.java              # 소설 작품 개별 메타데이터 정보 모델링 클래스
└── icon
├── decor.png          # 상세창 하단 데코레이션 일러스트 리소스
├── no_cover.png       # 등록된 표지가 없을 때 출력되는 Fallback 템플릿
├── kakao.png          # 카카오페이지 플랫폼 로고 이미지
└── series.png         # 네이버 시리즈 플랫폼 로고 이미지
