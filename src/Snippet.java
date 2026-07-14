import java.time.LocalDateTime;

public class Snippet {
    private String title;                   //단편/썰 제목
    private String author;                  //작가 이름(닉네임)
    private String platform;                //출처 플랫폼(포스타입, 트위터 등등)
    private String folderPath;              //텍스트 파일이 저장된 로컬 폴더 경로
    private String parentWork;              //소속 원작 타이틀(좌측 카테고리 앵커 매핑용)

    private String snippetType;             //콘텐츠 유형([단편], [썰], [연작])
    private int wordCount;                  //글자 수(바이트 용량 기반 또는 실제 파싱 값)
    private String keywords;                //키워드 및 태그(쉼표로 구분: 회귀, 육아물 등)
    private String description;             //소제목
    private String lastReadDate;            //마지막 열람 일시("기록없음" 또는 타임스탬프 문자열)
    private boolean isFavorite;             //즐겨찾기(하트) 여부
    private long favoriteTimestamp;         //즐겨찾기 정렬용 시간 값
    private String createdDate;             //보관함 최초 등록 일시

    //단편/썰 등록 전용 확장 생성자
    public Snippet(String title, String author, String platform, String folderPath, String parentWork,
                   String snippetType, int wordCount, String keywords, String description,
                   String lastReadDate, boolean isFavorite){
        this.title = title;
        this.author = author;
        this.platform = platform;
        this.folderPath = folderPath;
        this.parentWork = parentWork.isEmpty() ? "기타 단편" : parentWork; // 원작 공백 시 예외 처리
        this.snippetType = snippetType;
        this.wordCount = wordCount;
        this.keywords = keywords;
        this.description = description;
        this.lastReadDate = lastReadDate;
        this.isFavorite = isFavorite;
        this.favoriteTimestamp = 0;
        this.createdDate = LocalDateTime.now().toString();
    }
    // [Getter 및 Setter 데이터 통로 개방]
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getFolderPath() { return folderPath; }
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }

    public String getParentWork() { return parentWork; }
    public void setParentWork(String parentWork) { this.parentWork = parentWork; }

    public String getSnippetType() { return snippetType; }
    public void setSnippetType(String snippetType) { this.snippetType = snippetType; }

    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLastReadDate() { return lastReadDate; }
    public void setLastReadDate(String lastReadDate) { this.lastReadDate = lastReadDate; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public long getFavoriteTimestamp() { return favoriteTimestamp; }
    public void setFavoriteTimestamp(long favoriteTimestamp) { this.favoriteTimestamp = favoriteTimestamp; }

    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }
}
