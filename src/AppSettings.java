import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;

public class AppSettings {
    private static final String SETTINGS_FILE = "C:\\novel\\app_settings.txt";

    //설정 값 기억용 전역 변수
    private String installDate;         //프로그램 최초 설치 날짜
    private String defaultTheme;        // 기본 시작 테마(화이트/베이지/블랙)
    private String defaultFontName;     // 기본 시작 글꼴
    private int defaultFontSize;        // 기본 텍스트 크기
    private ArrayList<String> customPlatform;   //사용자가 추가한 커스텀 플랫폼 리스트

    //사용자가 플랫폼별로 등록한 아이콘 이미지 경로를 매핑 저장할 동적 리스트 상자
    private ArrayList<String> customPlatformIcons = new ArrayList<>();

    //싱글톤 패턴 적용(프로그램 전체에서 단 하나의 설정 객체만 공유)
    private static AppSettings instance;

    // 줄 간격, 문단 간격 변수
    private double lineHeight;      // 줄 간격(기본 1.6)
    private int paragraphSpacing;   //문단 간격(기본 10)

    public static AppSettings getInstance(){
        if(instance == null){
            instance = new AppSettings();
            instance.loadSettings();    //객체 생성 시 디스크 파일 자동 로드
        }
        return instance;
    }

    private AppSettings(){
        //파일이 없을 때 지정될 최초 초기 표준 사양 기본값 설정
        this.installDate = LocalDate.now().toString();
        this.defaultTheme = "라이트(흰색)";
        this.defaultFontName = "맑은 고딕";
        this.defaultFontSize = 16;
        this.customPlatform = new ArrayList<>(Arrays.asList("네이버 시리즈", "카카오페이지", "조아라", "문피아"));
        this.lineHeight = 1.6;
        this.paragraphSpacing = 10;

        //기본 4대 플랫폼 시스템 내장 기본 아이콘 경로로 초기 세팅
        this.customPlatformIcons = new ArrayList<>(Arrays.asList(
                "C:\\novel\\icon\\series.png",
                "C:\\novel\\icon\\kakao.png",
                "C:\\novel\\icon\\joara.png",
                "C:\\novel\\icon\\munpia.png"
        ));
    }

    //디스크 파일 영구 저장 로직
    public void saveSettings(){
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(SETTINGS_FILE))){
            String platformJoin = String.join(",", customPlatform);
            //아이콘 경로 목록도 쉼표로 결합
            String iconJoin = String.join(",", customPlatformIcons);

            //모든 속성을 파이프라인 기호(|)로 정밀 래핑하여 인쇄
            String line = installDate + "|" +
                    defaultTheme + "|" +
                    defaultFontName + "|" +
                    defaultFontSize + "|" +
                    platformJoin + "|" +
                    iconJoin + "|" +
                    lineHeight + "|" +
                    paragraphSpacing;

            bw.write(line);
            System.out.println(">> 환경 설정 파일 세이브 동기화 성공");
        } catch(IOException e){
            System.out.println("환경 설정 저장 실패: " + e.getMessage());
        }
    }

    //디스크 파일 로드 복원 로직
    public void loadSettings(){
        File file = new File(SETTINGS_FILE);
        if(!file.exists()){
            saveSettings(); //파일이 없으면 초기 기본값 사양으로 파일 자동 개설
            return;
        }

        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String line = br.readLine();
            if(line != null){
                String[] data =line.split("\\|", -1);
                if(data.length >= 8){
                    this.installDate = data[0];
                    this.defaultTheme = data[1];
                    this.defaultFontName = data[2];
                    this.defaultFontSize = Integer.parseInt(data[3]);

                    //플랫폼 목록 복원 파싱
                    this.customPlatform.clear();
                    if(!data[4].trim().isEmpty()){
                        String[] pArr = data[4].split(",");
                        for(String p : pArr){
                            this.customPlatform.add(p.trim());
                        }
                    }

                    //6번째 칸(|) 데이터가 존재하면 아이콘 이미지 경로 데이터 복원 복구 가동
                    this.customPlatformIcons.clear();
                    if(data.length >= 6 && !data[5].trim().isEmpty()){
                        String[] iArr = data[5].split(",");
                        for(String i : iArr){
                            this.customPlatformIcons.add(i.trim());
                        }
                    }

                    this.lineHeight = Double.parseDouble(data[6]);
                    this.paragraphSpacing = Integer.parseInt(data[7]);
                }
            }
        } catch(Exception e){
            System.out.println("환경 설정 로드 실패 (기본값 전환): " + e.getMessage());
        }
    }

    //[Getter / Setter 데이터 파이프라인 포트 개방]
    public String getInstallDate() { return installDate; }
    public void setInstallDate(String installDate){ this.installDate = installDate; }

    public String getDefaultTheme() { return defaultTheme; }
    public void setDefaultTheme(String defaultTheme) { this.defaultTheme = defaultTheme; }

    public String getDefaultFontName() { return defaultFontName; }
    public void setDefaultFontName(String defualtFontName) { this.defaultFontName = defualtFontName; }

    public int getDefaultFontSize() { return defaultFontSize; }
    public void setDefaultFontSize(int defaultFontSize) { this.defaultFontSize = defaultFontSize; }

    public double getLineHeight() { return lineHeight; }
    public void setLineHeight(double lineHeight) { this.lineHeight = lineHeight; }

    public int getParagraphSpacing() { return paragraphSpacing; }
    public void setParagraphSpacing(int paragraphSpacing) { this.paragraphSpacing = paragraphSpacing; }

    public ArrayList<String> getCustomPlatforms() { return customPlatform; }

    //아이콘 경로 리스트를 반환할 Getter 포트 개방
    public ArrayList<String> getCustomPlatformIcons() { return customPlatformIcons; }

    //플랫폼 항목 및 아이콘 경로를 함께 패킹하여 등록하는 전용 포트로 개조
    public void addPlatformWithIcon(String platformName, String iconPath){
        if(!customPlatform.contains(platformName) && !platformName.trim().isEmpty()){
            customPlatform.add(platformName.trim());
            //아이콘 경로가 공백이면 "none" 플래그 가드 주입
            customPlatformIcons.add(iconPath.trim().isEmpty() ? "none" : iconPath.trim());
            saveSettings(); //추가 즉시 디스크 영구 동기화
        }
    }
}
