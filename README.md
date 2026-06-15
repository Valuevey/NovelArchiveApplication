# 📚 소설 개인 보관함 프로그램 (Novel Bookshelf)

## 📖 프로젝트 소개

Java Swing을 이용하여 제작한 '개인용 소설 텍스트 파일 통합 관리 시스템'입니다.

사용자는 로컬 하드디스크에 보유 중인 소설 텍스트 회차 파일들을 프로그램에 연동하고, 로컬 텍스트 파일을 편리하게 열람할 수 있습니다.

---


## 📂 프로젝트 구조
```text
src
├── Main.java
├── BookShelfPage.java
├── NovelDetailPage.java
├── AddNovelDialog.java
├── AppSettings.java
└── Novel.java


### Main.java
프로그램 가동 시 메인 보관함 객체를 생성하고 주 프레임 화면을 출력하는 시작점 클래스

### BookShelfPage.java
전체 작품 카드 배치, 하트(즐겨찾기) 토글 소팅 및 키워드 태그 기반 실시간 검색 처리

### AddNovelDialog.java
소설 메타데이터 입력 다이얼로그 및 7개 이상 플랫폼 탐지 시 독립 팝업 레이어 구동

### Novel.java
제목, 작가, 장르, 플랫폼, 폴더 경로, 표지 주소, 키워드, 소개글 등 개별 소설 개체의 속성 데이터 정의

### NovelDetailPage.java
소설 텍스트 폴더 정밀 스캔, 책갈피 메모 리스킨 뷰 제공 및 정렬 필터 연동 제어

### NovelViewerPage.java
소설 본문 텍스트 뷰어 화면 및 독서 편의 기능 제어

### AppSettings.java
유저가 직접 등록하고 사용하는 플랫폼 리스트 보존 및 설정 데이터 연동

### 1. 메인 서재 보관함 화면
> **[여기에 메인 서재 화면 스크린샷 이미지 주소 또는 파일 삽입]**
- 사용자가 등록한 소설 카드들이 플랫하게 나열되며 완결 배지 및 플랫폼 로고가 정밀 도킹 출력됩니다.
