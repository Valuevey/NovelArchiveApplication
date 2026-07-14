public class Novel {
    private String title;
    private String author;
    private String genre;
    private String platforms;
    private String folderPath;
    private String coverPath;

    private String keywords;        //회귀, 먼치킨, 판타지 등등
    private String description;     //작품 소개글
    private String lastReadDate;    //마지막으로 본 날짜 "2026-06-04" 또는 "기록 없음"
    private boolean isFavorite;     //좋아요 활성화 여부

    private long favoriteTimestamp = 0;     //[좋아요]를 누른 시점을 초 단위로 기억할 타임스탬프 변수

    private boolean isCompleted;            //작품의 완결 상태로 추적할 플래그 변수

    //등록 히스토리 추적을 위한 작품 최초 등록일 필드 신설'
    private String createdDate;

    //연재중단 상태를 추적할 플래그 변수
    private boolean isHiatus;


    //전면 확장된 생성자
    public Novel(String title, String author, String genre, String platform, String folderPath, String coverPath,
                 String keywords, String description, String lastReadDate, boolean isFavorite){
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.platforms = platform;
        this.folderPath = folderPath;
        this.coverPath = coverPath;

        this.keywords = keywords;
        this.description = description;
        this.lastReadDate = lastReadDate;
        this.isFavorite = isFavorite;

        this.isCompleted = false;       //기본값은 연재중(false)로 세팅
        this.isHiatus = false;
        this.createdDate = java.time.LocalDateTime.now().toString(); //년-월-일 시각을 문자열로 배정
    }

    //데이터를 꺼내쓰기 위한 Getter 메서드들
    public String getTitle(){ return title; }
    public String getAuthor(){ return author; }
    public String getGenre(){ return genre; }
    public String getPlatform(){ return platforms; }
    public String getFolderPath(){ return folderPath; }
    public String getCoverPath(){ return coverPath; }

    public String getKeywords(){ return keywords; }
    public String getDescription(){ return description; }
    public String getLastReadDate(){ return lastReadDate; }

    public boolean isCompleted() { return isCompleted; }

    public String getCreatedDate() { return createdDate; }

    public boolean isHiatus() { return isHiatus; }


    //Setter 메서드들
    public void setLastReadDate(String lastReadDate) { this.lastReadDate = lastReadDate; }  //날짜 갱신용
    public boolean isFavorite(){ return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }    //좋아요 스위칭용

    public long getFavoriteTimestamp() { return favoriteTimestamp; }
    public void setFavoriteTimestamp(long favoriteTimestamp) { this.favoriteTimestamp = favoriteTimestamp; }

    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setPlatforms(String platforms) { this.platforms = platforms; }
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }
    public void setCoverPath(String coverPath) { this.coverPath = coverPath; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public void setDescription(String description) { this.description = description; }

    public void setCompleted(boolean completed) { this.isCompleted = completed; }

    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }

    public void setHiatus(boolean hiatus) { this.isHiatus = hiatus; }

}
