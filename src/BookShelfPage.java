import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;

public class BookShelfPage{
    private JFrame mainFrame;
    private JPanel libraryGridPanel;

    //우측 메인 영역의 화면 전환을 통제할 CardLayout 인프라
    private JPanel cardsContainer;
    private CardLayout cardLayout;
    private JPanel settingsPanel;

    //등록된 소설 객체들을 관리할 리스트
    private ArrayList<Novel> novelList = new ArrayList<>();
    private final java.util.Map<String, ImageIcon> coverImageCache = new java.util.HashMap<>(); //표지 리사이징 캐시
    //메타데이터가 저장될 파일 경로
    private final String LIBRARY_DATA_FILE = "C:\\novel\\library_data.txt";



    //GridBagLayout에서 바둑판 배열의 행과 열을 추적하기 위한 카운터 변수
    private int cardCount = 0;

    //실시간 필터링을 위한 상단바 콤보박스 및 검색창 멤버 변수 승격
    private JComboBox<String> platformCombo;
    private JComboBox<String> sortCombo;
    private JTextField searchField;

    //실시간 필터링된 작품 개수를 출력할 전역 라벨 객체 신설
    private JLabel lblTotalCounter;

    private JButton btnMyLibrary;

    // 단편/썰 전용 컴포넌트
    private JButton btnShortStoryLibrary;   //단편/썰 목록 버튼
    private ShortStoryPanel shortStoryPanel;    //신설할 독립 클래스 패널

    private JButton btnParodyLibrary;   // 패러디 서재 전용
    private ParodyPanel parodyPanel;

    //현재 선택된 좌측 메뉴 상태를 추적할 플래그 변수
    public String currentMenu = "내 서재";

    private String currentStatusTab = "전체";
    private JButton btnTabAll, btnTabOngoing, btnTabCompleted, btnTabHiatus;

    public void openBookShelf(){
        //로컬 디렉터리 자동 생성 가드
        try{
            File baseDir = new File("C:\\novel");
            File iconDir = new File("C:\\novel\\icon");
            File novelsDir = new File("C:\\novel\\novels");
            File coversDir = new File("C:\\novel\\covers");

            // 장편/단편,썰/패러디 분류
            String baseRoot = "C:\\novel\\novels\\";
            File novelListDir = new File(baseRoot + "novel_list");
            File shortStoriesDir = new File(baseRoot + "short_stories");
            File parodiesDir = new File(baseRoot + "parodies");

            // 디렉터리 스캔 후 부재 시 일괄 연쇄 자동 생성 가드
            if(!baseDir.exists()){ baseDir.mkdirs(); } //C 드라이브에 novel 폴더가 없으면 자동 생성
            if(!iconDir.exists()){ iconDir.mkdirs(); }  // novel 폴더에 로고 아이콘용 icon 폴더가 없으면 자동 생성
            if(!coversDir.exists()){ coversDir.mkdirs(); } //cover 폴더가 없으면 생성

            //사용자 투입 및 자동 생성 전용 자식 폴더 인프라 장착
            if(!novelListDir.exists()) novelListDir.mkdirs();
            if(!shortStoriesDir.exists()) shortStoriesDir.mkdirs();
            if(!parodiesDir.exists()) parodiesDir.mkdirs();

        } catch(Exception e){
            System.out.println("디렉터리 선행 생성 실패: "+ e.getMessage());
        }
        //프로그램 시작과 동시에 환경설정 데이터를 메로리에 복원 로드
        AppSettings.getInstance();

        //1. 메인 창 생성
        mainFrame = new JFrame("모던 웹소설 보관함 v2026.1.1");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1250, 740); //가로가 세로보다 크게
        mainFrame.setLocationRelativeTo(null);

        //메인 창의 레이아웃을 BorderLayout(상하좌우 분할)으로 설정
        mainFrame.setLayout(new BorderLayout());

        //오른쪽 메인 카드 전환 엔진 조립 구역
        cardLayout = new CardLayout();
        cardsContainer = new JPanel(cardLayout);

        //2. [왼쪽 구역] 메뉴 바 패널 생성(내서재, 좋아요, 설정 등등)
        JPanel sideMenuPanel = new JPanel();
        sideMenuPanel.setBackground(Color.WHITE);
        sideMenuPanel.setPreferredSize(new Dimension(200, 600));  //가로 폭을 200으로 고정

        //메뉴 버튼들을 위에서 아래로 쌓기 위해 BoxLayout(y측 방향) 설정
        sideMenuPanel.setLayout(new BoxLayout(sideMenuPanel, BoxLayout.Y_AXIS));

        //메뉴 항목 컴포넌트 생성
        JLabel lblMenuTitle = new JLabel("≡ 웹소설 보관함");
        lblMenuTitle.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        lblMenuTitle.setForeground(new Color(25, 30, 40));  //깊이 있는 다크 차폴 색

        //BoxLayout 정렬 축 충돌을 방지하기 위해 정렬 기준을 하단 단추들과 동일하게 센터로 맞춤
        lblMenuTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        //상하좌우 여백주기
        lblMenuTitle.setBorder(BorderFactory.createEmptyBorder(35, 20, 25, 20));

        //보관함 버트
        btnMyLibrary = new JButton("내 서재");
        JButton btnMyFavorites = new JButton("좋아요");
        JButton btnSettings = new JButton("환경 설정");
        btnShortStoryLibrary = new JButton("단편/썰 서재");
        btnParodyLibrary = new JButton("패러디 서재");

        //버튼 내부의 아이콘 그래픽을 직접 드로잉
        JButton[] menuButtons = {btnMyLibrary, btnMyFavorites, btnShortStoryLibrary, btnParodyLibrary, btnSettings};
        for(JButton btn : menuButtons){
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setPreferredSize(new Dimension(170, 42));   //버튼 크기 최적화 수치 갱신
            btn.setMaximumSize(new Dimension(170, 42));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setBackground(Color.WHITE);
            btn.setForeground(UiStyle.COLOR_TEXT_INACTIVE);
            btn.setFont(UiStyle.FONT_BOLD_13);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            //왼쪽 마진 패딩을 주어 글자가 아이콘 그래픽 우측으로 밀리도록 유도
            btn.setBorder(BorderFactory.createEmptyBorder(0, 48, 0, 0));
            btn.setHorizontalAlignment(SwingConstants.LEFT);    //글자 정렬선 좌측 고정

            //목표 시안과 일치하는 미니멀 아이콘을 직접 그리는 통합 커스텀 UI 주입
            btn.setUI(new javax.swing.plaf.basic.BasicButtonUI(){
                @Override
                public void paint(Graphics g, JComponent c){
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // 1. 선택된 메뉴의 은은한 연청록색 배경 라운드 칠하기
                    g2.setColor(c.getBackground());
                    g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 10, 10);

                    // 2. 버튼 텍스트 내용별 맞춤 고해상도 벡터 아이콘 개별 드로잉
                    JButton b = (JButton) c;
                    if(b.getForeground().getGreen() > 100){
                        g2.setColor(UiStyle.COLOR_ACCENT);    //선택 상태: 선명한 청록색 아이콘
                    } else{
                        g2.setColor(UiStyle.COLOR_ICON_INACTIVE);  //비선탤 상태: 차분한 회색 아이콘
                    }

                    // 3. 버튼 텍스트 내용별 맞춤 고해상도 벡터 아이콘 개별 드로잉
                    String btnText = b.getText();
                    // 부드러운 선 표현을 위해 굵기 2.0f 및 끝점 라운딩 처리 고정
                    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                    if(btnText.equals("내 서재")){
                        //책 모양 아이콘 그리기(왼쪽, 오른쪽 페이지 수치 연산 드로잉)
                        //곡선(Arc)을 이용하여 펼쳐진 책 아이콘 묘사

                        //왼쪽 페이지
                        g2.drawArc(18, 16, 10, 6, 0, 180);
                        g2.drawLine(18, 19, 18, 26);
                        g2.drawLine(28, 19, 28, 26);
                        g2.drawArc(18, 23, 10, 6, 0, -180);

                        //오른쪽 페이지
                        g2.drawArc(28, 16, 10, 6, 0, 180);
                        g2.drawLine(37, 19, 37, 26);
                        g2.drawArc(28, 23, 10, 6, 0, -180);

                        //중간 책갈피 기둥선
                        g2.drawLine(28, 16, 28, 24);

                    } else if(btnText.equals("좋아요")){
                        //하트 모양 아이콘 정밀 좌표 계산 드로잉
                        java.awt.geom.Path2D.Double heart = new java.awt.geom.Path2D.Double();
                        heart.moveTo(28, 17);
                        //왼쪽 반원 곡선부
                        heart.quadTo(23, 11, 18, 15);
                        heart.quadTo(14, 20, 20, 25);
                        //하단 정점 낙하선
                        heart.lineTo(28, 31);
                        //우측 하단 대칭선
                        heart.lineTo(36, 25);
                        heart.quadTo(42, 20, 38, 15);
                        //우측 상단 반원 곡선부 마감
                        heart.quadTo(33, 11, 28, 17);

                        g2.draw(heart);

                    } else if(btnText.equals("단편/썰 서재")){
                        //뒤쪽 종이 효과
                        g2.drawRoundRect(22, 14, 14, 16, 3, 3);
                        //앞쪽 겹쳐진 종이 효과
                        g2.setColor(c.getBackground());
                        g2.fillRect(17, 18, 14, 16);
                        if(b.getForeground().getGreen() > 100){
                            g2.setColor(UiStyle.COLOR_ACCENT);
                        } else{
                            g2.setColor(UiStyle.COLOR_ICON_INACTIVE);
                        }
                        g2.drawRoundRect(17, 18, 14, 16, 3, 3);
                        //종이 내부 줄무늬 텍스트 선 묘사
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawLine(21, 23, 27, 23);
                        g2.drawLine(21, 27, 25, 27);

                    } else if(btnText.equals("패러디 서재")){
                        g2.drawRoundRect(16, 14, 15, 17, 2, 2);
                        g2.drawLine(20, 20, 27, 20);
                        g2.drawLine(20, 24, 25, 24);
                        //대각선 깃펜 촉 묘사
                        g2.setStroke(new BasicStroke(2.0f));
                        g2.drawLine(28, 26, 34, 15);
                    } else if(btnText.equals("환경 설정")) {
                        //톱니바퀴 모양 아이콘 컴팩트 드로잉
                        int centerX = 28;
                        int centerY = 21;
                        int r1 = 5;     //내경 원 반경
                        int r2 = 9;     //외경 원 반경
                        int r3 = 12;    //톱니 돌기 끝단 반경

                        //메인 중심 이중 원 드로잉
                        g2.drawOval(centerX - r1, centerY - r1, r1 * 2, r1 * 2);
                        g2.drawOval(centerX - r2, centerY - r2, r2 * 2, r2 * 2);

                        //45도 간격으로 원주를 순회하며 8개의 정밀 라운드 돌기 배치
                        for(int i=0; i<8; i++){
                            double angle = Math.toRadians(i*45);
                            double cos = Math.cos(angle);
                            double sin = Math.sin(angle);

                            int x2 = (int) (centerX + r2 * cos);
                            int y2 = (int) (centerY + r2 * sin);
                            int x3 = (int) (centerX + r3 * cos);
                            int y3 = (int) (centerY + r3 * sin);

                            //외경선에서 돌기 끝단까지 바깥쪽으로 정밀 결합선 드로잉
                            g2.drawLine(x2, y2, x3, y3);
                        }
                    }
                    g2.dispose();
                    super.paint(g, c);
                }
            });
        }

        //메뉴 버튼 클릭 시 화면 리렌더링 및 하이라이트 배경색 스위칭 리스너를 결합
        btnMyLibrary.addActionListener(e -> {
            currentMenu = "내 서재";
            selectMenuButton(menuButtons, btnMyLibrary);

            sortCombo.setSelectedItem("최근 읽은 순");
            cardLayout.show(cardsContainer, "LIBRARY_CARD");
            refreshLibrary();
        });

        //좋아요 버튼 리스너
        btnMyFavorites.addActionListener(e -> {
            currentMenu = "좋아요";
            btnMyFavorites.setBackground(UiStyle.COLOR_MENU_HIGHLIGHT_BG);     //옅은 청록색 하이라이트 통일
            btnMyFavorites.setForeground(new Color(0, 140, 160));

            btnMyLibrary.setBackground(Color.WHITE);       //초기화
            btnMyLibrary.setForeground(UiStyle.COLOR_TEXT_INACTIVE);

            btnShortStoryLibrary.setBackground(Color.WHITE);
            btnShortStoryLibrary.setForeground(UiStyle.COLOR_TEXT_INACTIVE);

            btnParodyLibrary.setBackground(Color.WHITE);
            btnParodyLibrary.setForeground(UiStyle.COLOR_TEXT_INACTIVE);

            btnSettings.setBackground(Color.WHITE);
            btnSettings.setForeground(UiStyle.COLOR_TEXT_INACTIVE);

            //좋아요 메뉴로 들어오면 상단 정렬 피렅를 "좋아요 순"으로 자동 스위칭
            sortCombo.setSelectedItem("좋아요 순");

            //소설 목록 바둑판화면 카드를 정면 노출
            cardLayout.show(cardsContainer, "LIBRARY_CARD");
            refreshLibrary();   //필터링 엔진 가동
        });

        //환경 설정 버튼 리스너
        btnSettings.addActionListener(e -> {
            currentMenu = "환경 설정";
            btnSettings.setBackground(UiStyle.COLOR_MENU_HIGHLIGHT_BG);        //옅은 청록색 하이라이트 통일
            btnSettings.setForeground(UiStyle.COLOR_ACCENT);

            btnMyLibrary.setBackground(Color.WHITE);
            btnMyLibrary.setForeground(UiStyle.COLOR_TEXT_INACTIVE);

            btnShortStoryLibrary.setBackground(Color.WHITE);
            btnShortStoryLibrary.setForeground(UiStyle.COLOR_TEXT_INACTIVE);

            btnParodyLibrary.setBackground(Color.WHITE);
            btnParodyLibrary.setForeground(UiStyle.COLOR_TEXT_INACTIVE);

            btnMyFavorites.setBackground(Color.WHITE);
            btnMyFavorites.setForeground(UiStyle.COLOR_TEXT_INACTIVE);

            //환경설정 UI 리팩토링 갱신 메서드 원격 호출
            updateSettingsPanel();

            //대시보드 및 환경설정이 꽂혀있는 독립 설정카드로 화면 전환
            cardLayout.show(cardsContainer, "SETTINGS_CARD");
        });

        btnParodyLibrary.addActionListener(e -> {
            currentMenu = "패러디 서재";
            btnParodyLibrary.setBackground(UiStyle.COLOR_MENU_HIGHLIGHT_BG);
            btnParodyLibrary.setForeground(new Color(0, 140, 160));

            btnMyLibrary.setBackground(Color.WHITE); btnMyLibrary.setForeground(UiStyle.COLOR_TEXT_INACTIVE);
            btnMyFavorites.setBackground(Color.WHITE); btnMyFavorites.setForeground(UiStyle.COLOR_TEXT_INACTIVE);
            btnShortStoryLibrary.setBackground(Color.WHITE); btnShortStoryLibrary.setForeground(UiStyle.COLOR_TEXT_INACTIVE);
            btnSettings.setBackground(Color.WHITE); btnSettings.setForeground(UiStyle.COLOR_TEXT_INACTIVE);

            //독립 커스텀 패널 화면 노출 및 리프레시 엔진 구동
            cardLayout.show(cardsContainer, "PARODY_CARD");
            parodyPanel.refreshParodyLibrary();
        });

        //단편/썰 서재 버튼 리스너
        btnShortStoryLibrary.addActionListener(e -> {
            currentMenu = "단편/썰 서재";
            btnShortStoryLibrary.setBackground((UiStyle.COLOR_MENU_HIGHLIGHT_BG));
            btnShortStoryLibrary.setForeground(new Color(0, 140, 160));

            btnMyLibrary.setBackground(Color.WHITE);
            btnMyLibrary.setForeground(UiStyle.COLOR_TEXT_INACTIVE);

            btnMyFavorites.setBackground(Color.WHITE);
            btnMyFavorites.setForeground(UiStyle.COLOR_TEXT_INACTIVE);

            btnParodyLibrary.setBackground(Color.WHITE);
            btnParodyLibrary.setForeground(UiStyle.COLOR_TEXT_INACTIVE);

            btnSettings.setBackground(Color.WHITE);
            btnSettings.setForeground(UiStyle.COLOR_TEXT_INACTIVE);

            //독립 커스텀 패널 화면 노출 및 렌더링 동기화 호출
            cardLayout.show(cardsContainer, "SHORT_STORY_CARD");
            shortStoryPanel.refreshShortStoryLibrary();
        });


        //현재 선택된 메뉴이므로 '내 서재' 강조
        btnMyLibrary.setBackground(new Color(230, 245, 240));
        btnMyLibrary.setForeground(UiStyle.COLOR_ACCENT);

        //왼쪽 메뉴 패널에 컴포넌트 조립
        sideMenuPanel.add(lblMenuTitle);
        //정렬 밸런스를 맞추기 위한 여백용 인셋 래핑 패널 조립 적재
        JPanel menuWrapperPanel = new JPanel();
        menuWrapperPanel.setBackground(Color.WHITE);
        menuWrapperPanel.setLayout(new BoxLayout(menuWrapperPanel, BoxLayout.Y_AXIS));
        menuWrapperPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));  //좌우 15px 패딩 확보

        menuWrapperPanel.add(btnMyLibrary);
        menuWrapperPanel.add(Box.createVerticalStrut(8));       //버튼 사이의 물리 간격 단정화
        menuWrapperPanel.add(btnMyFavorites);
        menuWrapperPanel.add(Box.createVerticalStrut(8));
        menuWrapperPanel.add(btnShortStoryLibrary);
        menuWrapperPanel.add(Box.createVerticalStrut(8));
        menuWrapperPanel.add(btnParodyLibrary);
        menuWrapperPanel.add(Box.createVerticalStrut(8));
        menuWrapperPanel.add(btnSettings);

        sideMenuPanel.add(menuWrapperPanel);

        // 보관함 좌측 하단 꾸미기
        //1. 위쪽 버튼들과 하단 이미지 사이에 세로 여백 배치해서 이미지 밑으로 밀어냄
        sideMenuPanel.add(Box.createVerticalGlue());

        //2. 이미지 로드하고 스케일링하여 담아낼 전용 라벨 생성
        JLabel lblSideDecor = new JLabel();
        lblSideDecor.setAlignmentX(Component.CENTER_ALIGNMENT);

        //메뉴 패널 가로폭 및 마진 고려해서 데코 이미지 크기 조절
        int decorSize = 210;
        String decorPath = "C:\\novel\\icon\\decor.png";

        File decorFile = new File(decorPath);
        if(decorFile.exists()){
            try{
                ImageIcon decorIcon = new ImageIcon(decorPath);
                Image scaledDecor = decorIcon.getImage().getScaledInstance(decorSize, decorSize, Image.SCALE_SMOOTH);
                lblSideDecor.setIcon(new ImageIcon(scaledDecor));
            } catch(Exception e){
                System.out.println("데코 일러스트 스케일링 실패: " + e.getMessage());
            }
        } else{
            //파일이 지정된 경로에 배치되지 않은 상태일 때 레이아웃 붕괴를 막기 위해 투명 마진 박스로 대체 작동
            lblSideDecor.setPreferredSize(new Dimension(decorSize, decorSize));
            lblSideDecor.setMaximumSize(new Dimension(decorSize, decorSize));
        }

        //3. 이미지 라벨 하단이 프로그램 창 밑바닥에 너무 바짝 붙이 않도록 마진 패딩
        lblSideDecor.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        //좌측 뼈대 패널에 일러스트 최종 적재
        sideMenuPanel.add(lblSideDecor);

        //3. [오른쪽 구역] 상단 툴바 패널 생성(필터, 새소설 추가, 정렬, 검색창)
        JPanel libraryContentPanel = new JPanel(new BorderLayout());    //오른쪽 전체를 감쌀 패널
        libraryContentPanel.setBackground(Color.WHITE);

        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setBackground(Color.WHITE);
        toolbarPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 12, 15)); //우측 정렬, 여백 설정

        //환경설정 앱세팅 연동: 커스텀 플랫폼 어레이 리스트 데이터를 읽어와서 상단 콤보박스 아이템 동적 세팅
        ArrayList<String> loadPlatforms = AppSettings.getInstance().getCustomPlatforms();
        ArrayList<String> comboItem = new ArrayList<>();
        comboItem.add("전체 플랫폼");
        comboItem.addAll(loadPlatforms);

        //전체 플랫폼 콤보박스를 텍두리가 부드럽게 깎인 라운드 플랫 박스로 개조
        platformCombo = new JComboBox<>(comboItem.toArray(new String[0])){
            @Override
            protected void paintComponent(Graphics g){
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                g2. setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                //테두리 드로잉
                g2.setColor(new Color(0, 160, 160));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);

                g2.dispose();
            }
        };

        //내부 렌더링 동기화를 위해 불투명 속성을 소거, 날카로운 기본 각진 선을 제거
        platformCombo.setOpaque(false);
        platformCombo.setBackground(Color.WHITE);
        platformCombo.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 5));
        platformCombo.setFocusable(false);  // ← 추가

        platformCombo.setRenderer(new javax.swing.DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus){
                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                c.setBackground(Color.WHITE);
                c.setForeground(UiStyle.COLOR_ACCENT);
                c.setFont(UiStyle.FONT_PLAIN_12);
                c.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return c;
            }
        });

        //기본 각진 화살표 단추 삭제, V 버튼으로 전격 스위칭
        platformCombo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI(){
            @Override
            protected JButton createArrowButton(){
                JButton button = new JButton(){
                    @Override
                    protected void paintComponent(Graphics g){
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        //청록색 꺽쇠 기호(V) 드로잉
                        g2.setColor(UiStyle.COLOR_ACCENT);
                        g2.drawLine(6, 13, 10, 17);
                        g2.drawLine(10, 17, 14, 13);
                        g2.dispose();
                    }
                };
                button.setBorderPainted(false);
                button.setContentAreaFilled(false);
                button.setOpaque(false);
                return button;
            }
        });

        JButton btnAddNovel = new JButton("+ 새 소설 추가"){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                //1. 배경 채우기
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                //2. 외곽선 테두리선 드로잉
                g2.setColor(new Color(0, 160, 160));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnAddNovel.setFont(UiStyle.FONT_BOLD_12);
        btnAddNovel.setForeground(new Color(0, 160, 160));
        btnAddNovel.setFocusPainted(false);
        btnAddNovel.setBorderPainted(false);                //각진 기본 외곽선 제거
        btnAddNovel.setContentAreaFilled(false);            //Swing 기본 사각 배경 인쇄 기능 제거
        btnAddNovel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAddNovel.setPreferredSize(new Dimension(115, 30));   //크기 고정

        String[] sortOption = {"최근 읽은 순", "제목순", "안 읽은 작품", "좋아요 순"};
        sortCombo = new JComboBox<>(sortOption){
            @Override
            protected void paintComponent(Graphics g){
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                //2. 테두리 드로잉(소프트 그레이)
                g2.setColor(UiStyle.COLOR_BORDER_GRAY);
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);

                g2.dispose();

            }
        };
        //내부 렌더링 동기화를 위해 불투명 속성을 소거, 날카로운 기본 각진 선을 제거
        sortCombo.setOpaque(false);
        sortCombo.setBackground(Color.WHITE);
        sortCombo.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 5));
        sortCombo.setFocusable(false);

        sortCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                c.setBackground(Color.WHITE);
                c.setForeground(new Color(60, 65, 75)); // 선택 글자색 다크 차콜 그레이
                c.setFont(UiStyle.FONT_PLAIN_12);
                c.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return c;
            }
        });

        //은은한 회색 미니멀 꺽쇠(V) 버튼으로 동기화
        sortCombo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI(){
            @Override
            protected JButton createArrowButton(){
                JButton button = new JButton(){
                    @Override
                    protected void paintComponent(Graphics g){
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        //회색 꺽쇠 기호(V) 드로잉
                        g2.setColor(Color.GRAY);
                        g2.drawLine(6, 13, 10, 17);
                        g2.drawLine(10, 17, 14, 13);
                        g2.dispose();
                    }
                };
                button.setBorderPainted(false);
                button.setContentAreaFilled(false);
                button.setOpaque(false);
                return button;
            }
        });

        sortCombo.addActionListener(e -> refreshLibrary());     //선택 시 리프레시 트리거

        JPanel searchContainer = new JPanel(new BorderLayout(6, 0));    //검색창과 버튼 사이에 마진 6px 확보
        searchContainer.setBackground(Color.WHITE); //보관함 배경색에 맞게 조율

        searchField = new JTextField("제목, 작가, 태그 검색...", 12){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

                //배경 페인팅
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                //테두리 드로잉
                g2.setColor(UiStyle.COLOR_BORDER_GRAY);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        searchField.setOpaque(false);   //커스텀 paintComponent 렌더링 동기화를 위해 불투명 수치 해제
        //내부 텍스트가 좌측 테두리에 너무 바짝 붙지 않도록 안쪽 공백 마진 패딩 배치
        searchField.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        searchField.setForeground(Color.GRAY);
        searchField.setFont(UiStyle.FONT_PLAIN_12);

        JButton btnCancelSearch = new JButton("취소");
        btnCancelSearch.setPreferredSize(new Dimension(30, 30));
        btnCancelSearch.setFont(UiStyle.FONT_BOLD_12);
        btnCancelSearch.setForeground(UiStyle.COLOR_ACCENT);
        btnCancelSearch.setFocusPainted(false);
        btnCancelSearch.setBorderPainted(false);
        btnCancelSearch.setContentAreaFilled(false);
        btnCancelSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelSearch.setMargin(new java.awt.Insets(0, 0, 0, 0));

        //최초 렌더링 시에는 취소버튼을 은닉
        btnCancelSearch.setVisible(false);

        searchContainer.setPreferredSize(new Dimension(195, 30));

        //가로 조립 패널에 최종 컴포넌트들을 좌우 정렬 도킹
        searchContainer.add(searchField, BorderLayout.CENTER);
        searchContainer.add(btnCancelSearch, BorderLayout.EAST);

        //검색창 포커스 이벤트 처리
        searchField.addFocusListener(new java.awt.event.FocusListener(){
            @Override
            public void focusGained(java.awt.event.FocusEvent e){
                if(searchField.getText().equals("제목, 작가, 태그 검색...")){
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
                btnCancelSearch.setVisible(true);
                searchContainer.revalidate();
                searchContainer.repaint();
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e){
                if(searchField.getText().trim().isEmpty()){
                    searchField.setForeground(Color.GRAY);
                    searchField.setText("제목, 작가, 태그 검색...");
                    btnCancelSearch.setVisible(false);
                    searchContainer.revalidate();
                    searchContainer.repaint();
                }
            }
        });

        //엔터키로 검색 실행
        searchField.addActionListener(e -> refreshLibrary() );

        btnCancelSearch.addActionListener(e -> {
            searchField.setForeground(Color.GRAY);
            searchField.setText("제목, 작가, 태그 검색...");
            refreshLibrary();   //필터 원상복구
            btnCancelSearch.setVisible(false);
            searchContainer.revalidate();
            searchContainer.repaint();
        });

        //검색 안내 기능: 마우스를 올리면 검색 팁 말풍성이 출력되는 ⓘ 안내판 라벨
        JLabel lblSearchInfo = new JLabel("ⓘ");
        lblSearchInfo.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblSearchInfo.setForeground(new Color(120, 130, 140));  //차분한 그레이블루 색상
        lblSearchInfo.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JWindow tipWindow = new JWindow(mainFrame);
        //외곽 윈도우 자체의 사각 배경을 투명하게 선언하여 깎인 모서리 바깥쪽이 비쳐 보이도록 조치
        tipWindow.setBackground(new Color(0, 0, 0, 0));

        //내부 컴포넌트들을 담을 패널 개설, 내부 그래픽을 둥글게 깍아 드로잉
        JPanel tipContainer = new JPanel(){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D)  g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                //배경
                g2.setColor(new Color(248, 249, 251));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                //테두리
                g2.setColor(new Color(220, 225, 230));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

                g2.dispose();
            }
        };

        tipContainer.setLayout(new BoxLayout(tipContainer, BoxLayout.Y_AXIS));
        tipContainer.setOpaque(false);
        tipContainer.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        // 2. 줄바꿈 단위로 라벨을 분리하여 깔끔하게 세로로 적재
        String[] infoLines = {
                "보관함 검색 팁 안내",
                "",
                "ㆍ@작가이름 (ex: @홍길동)",
                "ㆍ#키워드 (ex: #힐링물),",
                "ㆍ제목만 입력(ex: 콩쥐밭쥐)",
                "",
                "※ 부분 일치 검색 지원."
        };

        for (int i = 0; i < infoLines.length; i++) {

            JLabel lineLabel = new JLabel(infoLines[i]);

            if (i == 0) {
                lineLabel.setFont(UiStyle.FONT_BOLD_13);
                lineLabel.setForeground(UiStyle.COLOR_ACCENT);
            }
            else if (infoLines[i].contains("부분 일치")) {
                lineLabel.setFont(UiStyle.FONT_PLAIN_11);
                lineLabel.setForeground(new Color(130, 130, 130));
            }
            else {
                lineLabel.setFont(UiStyle.FONT_PLAIN_12);
                lineLabel.setForeground(new Color(60, 60, 60));
            }
            tipContainer.add(lineLabel);
        }
        tipWindow.add(tipContainer);
        tipWindow.pack();

        //3. 마우스를 올렸을 때 중앙 하단 좌표를 계산하여 즉시 팝업 가동
        lblSearchInfo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                //모니터 전체 기준 아이콘 절대 위치 좌표를 실시간 추출
                Point origin = lblSearchInfo.getLocationOnScreen();

                //X축: 아이콘 정중앙 점핑 후 팝업창 절반 크기만큼 좌측으로 백업
                int targetX = origin.x + lblSearchInfo.getWidth() - tipWindow.getWidth();
                // Y축: 아이콘 아래로 22픽셀 낙하
                int targetY = origin.y + lblSearchInfo.getHeight() + 4;

                tipWindow.setLocation(targetX, targetY);
                tipWindow.setVisible(true); //창 띄우기
            }
            @Override
            public void mouseExited(MouseEvent e){
                //마우스가 아이콘 영역을 가출하여 벗어나는 즉시 화면에서 소멸
                tipWindow.setVisible(false);
            }
        });

        toolbarPanel.setLayout(new BorderLayout());
        toolbarPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));

        //좌측에 배치할 대제목 + 카운터 배지 결합 바구니 생성
        JPanel leftTitleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftTitleGroup.setOpaque(false);

        JLabel lblPageTitle = new JLabel("내 서재");
        lblPageTitle.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        lblPageTitle.setForeground(new Color(25, 30, 40));


        //통계 카운터 라벨의 디자인을 정의하고 패널 최좌측에 배치
        lblTotalCounter = new JLabel("작품 수: 0개"){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                //1. 배경을 은은하고 연한 청록색으로 채우기(모양은 알약 모양)
                g2.setColor(UiStyle.COLOR_MENU_HIGHLIGHT_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblTotalCounter.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        lblTotalCounter.setForeground(UiStyle.COLOR_ACCENT); // 청록색
        lblTotalCounter.setOpaque(false);
        lblTotalCounter.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        leftTitleGroup.add(lblPageTitle);
        leftTitleGroup.add(lblTotalCounter);

        //우측에 기존 조잦ㄱ 컴포넌트들을 모아줄 서브 패널 생성
        JPanel rightButtonGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightButtonGroup.setOpaque(false);

        //기존 컴포넌트들을 우측 바구니에 차례대로 담음
        rightButtonGroup.add(platformCombo);
        rightButtonGroup.add(btnAddNovel);
        rightButtonGroup.add(sortCombo);
        rightButtonGroup.add(searchContainer);
        rightButtonGroup.add(lblSearchInfo);

        //최종 툴바 패널의 동서벽면에 교차 도킹 완료
        toolbarPanel.removeAll();
        toolbarPanel.add(leftTitleGroup, BorderLayout.WEST);
        toolbarPanel.add(rightButtonGroup, BorderLayout.EAST);

        //기본 JComboBox들의 각진 모서리와 내부 패딩을 마감 조정
        platformCombo.setFont(UiStyle.FONT_BOLD_12);
        platformCombo.setForeground(UiStyle.COLOR_ACCENT);

        sortCombo.setFont(UiStyle.FONT_BOLD_12);
        sortCombo.setForeground(new Color(60, 65, 75));

        platformCombo.setPreferredSize(new Dimension(130, 32));
        sortCombo.setPreferredSize(new Dimension(135, 32));

        //4. [오른쪽 구역] 소설 카드들이 꽂힐 서재 본문 패널 생성
        libraryGridPanel = new JPanel();
        libraryGridPanel.setBackground(UiStyle.COLOR_BG_LIGHT);

        //바둑판 모양 레이아웃 설정 : 1줄에 3칸씩, 가로세로 가격 20픽셀
        libraryGridPanel.setLayout(new GridBagLayout());

        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.setBackground(UiStyle.COLOR_BG_LIGHT);
        gridWrapper.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JPanel innerAlignPanel = new JPanel(new BorderLayout());
        innerAlignPanel.setBackground(UiStyle.COLOR_BG_LIGHT);
        innerAlignPanel.add(libraryGridPanel, BorderLayout.WEST);

        gridWrapper.add(innerAlignPanel, BorderLayout.NORTH);

        JScrollPane libraryScrollPane = new JScrollPane(gridWrapper);

        libraryScrollPane.setBackground(UiStyle.COLOR_BG_LIGHT);
        libraryScrollPane.getViewport().setBackground(UiStyle.COLOR_BG_LIGHT);
        libraryScrollPane.setBorder(BorderFactory.createEmptyBorder());

        libraryScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel topWrapperPanel = new JPanel(new BorderLayout());
        topWrapperPanel.setBackground(Color.WHITE);
        topWrapperPanel.add(toolbarPanel, BorderLayout.NORTH);

        JPanel statusBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusBarPanel.setBackground(Color.WHITE);
        statusBarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 15, 0, 25),
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230))
        ));

        btnTabAll = createTabButton("전체");
        btnTabOngoing = createTabButton("연재중");
        btnTabCompleted = createTabButton("완결");
        btnTabHiatus = createTabButton("연재중단");
        updateTabStyles();

        statusBarPanel.add(btnTabAll);
        statusBarPanel.add(btnTabOngoing);
        statusBarPanel.add(btnTabCompleted);
        statusBarPanel.add(btnTabHiatus);

        topWrapperPanel.add(statusBarPanel, BorderLayout.SOUTH);
        libraryContentPanel.add(topWrapperPanel, BorderLayout.NORTH);

        libraryContentPanel.add(libraryScrollPane, BorderLayout.CENTER);

        //환경 설정 본체 가상 도화지 패널 개설
        settingsPanel = new JPanel(new BorderLayout());
        settingsPanel.setBackground(Color.WHITE);

        //카드 보드 컨테이너 상자에 2개의 메인 화면 분기 트랙을 도킹
        cardsContainer.add(libraryContentPanel, "LIBRARY_CARD");
        cardsContainer.add(settingsPanel, "SETTINGS_CARD");

        shortStoryPanel = new ShortStoryPanel(mainFrame, this);
        cardsContainer.add(shortStoryPanel, "SHORT_STORY_CARD");

        parodyPanel = new ParodyPanel(mainFrame, this);
        cardsContainer.add(parodyPanel, "PARODY_CARD");

        //빈 바탕화면이나 카드 스크롤 영역 클릭 시 검색창 커서 안뜨게
        //우측 메인 패널이 검색창의 포커스를 뺏게 주권 자격 부여
        libraryContentPanel.setFocusable(true);

        //1. 툴바 아래 소설 카드들이 배치되는 빈 배경 공간을 클릭했을 때 포커스 강제 회수
        libraryContentPanel.addMouseListener(new java.awt.event.MouseAdapter(){
            @Override
            public void mousePressed(java.awt.event.MouseEvent e){
                libraryContentPanel.requestFocusInWindow();
            }
        });

        //2. 소설 카드가 꽂히는 내부 스크롤뷰 레이어 내부의 빈 틈새를 클릭했을 때도 포커스 강제 회수
        libraryScrollPane.addMouseListener(new java.awt.event.MouseAdapter(){
            @Override
            public void mousePressed(java.awt.event.MouseEvent e){
                libraryContentPanel.requestFocusInWindow();
            }
        });

        //3. 작품 카드가 적재되는 바둑판 그리드 패널 자체의 빈 마진 여백을 클릭햇을 때도 회수
        libraryGridPanel.addMouseListener(new java.awt.event.MouseAdapter(){
            @Override
            public void mousePressed(java.awt.event.MouseEvent e){
                libraryContentPanel.requestFocusInWindow();
            }
        });

        // 프로그램 시작 시 파일 로드
        System.out.println("로드 시작");
        loadLibraryData();
        System.out.println("로드 완료");

        // [필터 엔진 장착]: 컴포넌트의 상태가 변할 때마다 화면을 실시간 재배치하도록 이벤트 연결
        platformCombo.addActionListener(e -> refreshLibrary());
        sortCombo.addActionListener(e -> refreshLibrary());

        //새 소설 추가 버튼 클릭 시 팝업 열기
        btnAddNovel.addActionListener(e -> {
            AddNovelDialog dialog = new AddNovelDialog(mainFrame);
            dialog.setVisible(true);    //유저가 입력 마칠 때까지 대기(Modal)

            //팝업창이 닫힌 후 데이터 꺼내기
            Novel newNovel = dialog.getResultNovel();
            if(newNovel != null){
                novelList.add(newNovel);    //리스트에 추가
                addNovelCard(newNovel);     //화면에 카드 생성
                saveLibraryData();          //텍스트 파일에 변경 사항 저장
                refreshLibrary();           //소설 추가 시 화면 실시간 리프레시
            }
        });

        //5. 전체 레이아웃 조립하기
        mainFrame.add(sideMenuPanel, BorderLayout.WEST);  //왼쪽 구역 배치
        mainFrame.add(cardsContainer, BorderLayout.CENTER);    //오른쪽 구역 배치

        mainFrame.setVisible(true);
    }

    //환경 설정 메뉴 클릭
    private void updateSettingsPanel(){
        settingsPanel.removeAll();

        // 전체 패딩 마진 부여 및 레이아웃 정의
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        settingsPanel.setLayout(new BorderLayout(0, 15));

        //임시 변경 사항을 격리 보관할 설정 버퍼 변수
        final String[] tempTheme = { AppSettings.getInstance().getDefaultTheme() };
        final String[] tempFont = { AppSettings.getInstance().getDefaultFontName() };
        final int[] tempFontSize = { AppSettings.getInstance().getDefaultFontSize() };

        //[상단 0층 헤더 패널]: 프로그램 설치 및 사용 기한 출력
        JPanel topHeaderInfoPanel = new JPanel(new BorderLayout(12, 0));
        topHeaderInfoPanel.setBackground(new Color(238, 247, 247));

        //외곽 두께선 드로잉 대신 둥근 회색 엣지 라인으로 감쌈
        topHeaderInfoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 230, 230), 1, true),
                BorderFactory.createEmptyBorder(16, 16, 12, 16)
        ));

        //좌측 ⓘ 문양
        JLabel lblInfoIcon = new JLabel("ⓘ");
        lblInfoIcon.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        lblInfoIcon.setForeground(UiStyle.COLOR_ACCENT);

        String installDateStr = AppSettings.getInstance().getInstallDate(); //설치일 로드
        long dayBetween = ChronoUnit.DAYS.between(
                java.time.LocalDate.parse(installDateStr), java.time.LocalDateTime.now()
        ) + 1;  //오늘이 사용 1일차가 되도록 연산 보정

        JLabel lblProgramInfo = new JLabel("<html><body>프로그램 설치일: <b>" + installDateStr +
                "</b> &nbsp;&nbsp;|&nbsp;&nbsp; 사용 <font color='#008C8C'><b>" + dayBetween +
                "일째</b></font> 사용 중</body></html>");
        lblProgramInfo.setFont(new Font("맑은 고딕", Font.PLAIN,13));
        lblProgramInfo.setForeground(new Color(60, 65, 70));

        topHeaderInfoPanel.add(lblInfoIcon, BorderLayout.WEST);
        topHeaderInfoPanel.add(lblProgramInfo, BorderLayout.CENTER);
        settingsPanel.add(topHeaderInfoPanel, BorderLayout.NORTH);

        //[중앙 1층 메인 탭 패널 인프라 가동]
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UiStyle.FONT_BOLD_13);
        tabbedPane.setBackground(Color.WHITE);  //헤더 배경 화이트 스위칭
        tabbedPane.setOpaque(true);

        //탭 UI 시스템의 회색 외곽 격벽선을 제거하기 위해 플랫 패널 속성 연동 가이드 적용
        tabbedPane.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected Insets getTabAreaInsets(int tabPlacement) {
                // 🌟 테마 엔진이 Null을 읽지 않도록 안전한 기본 여백 인셋 데이터 객체를 수동 보정 반환합니다.
                return new Insets(0, 0, 0, 0);
            }

            @Override
            protected Insets getContentBorderInsets(int tabPlacement) {
                // 콘텐츠 바운더리 계산 영역 충돌 방지를 위한 안전 제로 인셋 주입
                return new Insets(0, 0, 0, 0);
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                // 기본 사각형 탭 선택 배경색 인쇄 무력화
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                // 구형 세로 격벽선 및 외곽 테두리선 소거
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // 콘텐츠 영역을 감싸는 우중충한 사각 큰 선 제거
            }

            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
                // 포커스 점선 사각형 소멸
            }
            @Override
            protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect) {
                super.paintTab(g, tabPlacement, rects, tabIndex, iconRect, textRect);

                if (tabIndex == tabbedPane.getSelectedIndex()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g2.setColor(UiStyle.COLOR_ACCENT);

                    int xStart = rects[tabIndex].x;
                    int yBottom = rects[tabIndex].y + rects[tabIndex].height - 4;
                    int width = rects[tabIndex].width;

                    g2.fillRect(xStart, yBottom, width, 3);
                    g2.dispose();
                }
            }

            @Override
            protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                if (isSelected) {
                    g2.setColor(UiStyle.COLOR_ACCENT);
                    g2.setFont(font.deriveFont(Font.BOLD));
                } else {
                    g2.setColor(new Color(60, 65, 75));
                    g2.setFont(font.deriveFont(Font.BOLD));
                }

                int textX = textRect.x;
                int textY = textRect.y + metrics.getAscent();
                g2.drawString(title, textX, textY);
                g2.dispose();
            }
        });

        // 탭[1] : 일반 설정 UI 컴포넌트 렌더링
        JPanel tabGeneral = new JPanel();
        tabGeneral.setBackground(Color.WHITE);
        tabGeneral.setLayout(new BoxLayout(tabGeneral, BoxLayout.Y_AXIS));
        tabGeneral.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        Color themeCyan = UiStyle.COLOR_ACCENT;
        Color borderGray = UiStyle.COLOR_BORDER_GRAY;

        //[기본 설정] 패널
        JPanel baseSettingCard = new JPanel(){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                //하얀 바탕
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);

                //연회색의 부드러운 라운드 테두리선
                g2.setColor(new Color(230, 235, 240));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        baseSettingCard.setOpaque(false);
        baseSettingCard.setLayout(new BoxLayout(baseSettingCard, BoxLayout.Y_AXIS));
        baseSettingCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 135));
        baseSettingCard.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        baseSettingCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        //카드 소제목 레이어 배치
        JLabel lblBaseTitle = new JLabel("기본 설정");
        lblBaseTitle.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblBaseTitle.setForeground(themeCyan);
        lblBaseTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        baseSettingCard.add(lblBaseTitle);
        baseSettingCard.add(Box.createVerticalStrut(12));   //타이틀과 본문 사이 공백 마진

        // (1) 테마 설정 가로 행 조립
        JPanel rowTheme = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rowTheme.setBackground(Color.WHITE);
        rowTheme.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowTheme.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel lblTheme = new JLabel("전체 기본 시작 테마 설정: ");
        lblTheme.setFont(UiStyle.FONT_PLAIN_13);
        lblTheme.setForeground(UiStyle.COLOR_LABEL_TEXT);
        lblTheme.setPreferredSize(new Dimension(220, 30));

        String[] themes = {"흰색(화이트)", "베이지색", "검정색(블랙)"};
        JComboBox<String> comboTheme = new JComboBox<>(themes){
            @Override
            protected void paintComponent(Graphics g){
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(borderGray);
                g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 10, 10);

                g2.dispose();
            }
        };
        comboTheme.setSelectedItem(AppSettings.getInstance().getDefaultTheme());
        comboTheme.setFont(UiStyle.FONT_PLAIN_12);
        comboTheme.setPreferredSize(new Dimension(160, 30));
        comboTheme.setOpaque(false);
        comboTheme.setBackground(Color.WHITE);
        comboTheme.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
        comboTheme.setFocusable(false);

        comboTheme.setUI(new javax.swing.plaf.basic.BasicComboBoxUI(){
            @Override
            protected JButton createArrowButton(){
                JButton button = new JButton(){
                    @Override
                    protected void paintComponent(Graphics g){
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.setColor(UiStyle.COLOR_ACCENT);

                        g2.drawLine(5, 13, 9, 17);
                        g2.drawLine(9, 17, 13, 13);
                        g2.dispose();
                    }
                };
                button.setBorderPainted(false);
                button.setContentAreaFilled(false);
                button.setOpaque(false);
                return button;
            }
        });
        comboTheme.setRenderer(new javax.swing.DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus){
                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                c.setBackground(Color.WHITE);
                c.setForeground(new Color(60, 65, 75));
                c.setFont(UiStyle.FONT_PLAIN_12);
                c.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return c;
            }
        });

        comboTheme.addActionListener(e -> {
            tempTheme[0] = (String) comboTheme.getSelectedItem();
        });

        rowTheme.add(lblTheme);
        rowTheme.add(comboTheme);
        baseSettingCard.add(rowTheme);
        baseSettingCard.add(Box.createVerticalStrut(10));

        // (2) 텍스트 사양 설정 행 조립
        JPanel rowFont = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rowFont.setBackground(Color.WHITE);
        rowFont.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowFont.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel lblFont = new JLabel("뷰어 기본 글꼴 및 크기 선행 지정: ");
        lblFont.setFont(UiStyle.FONT_PLAIN_13);
        lblFont.setForeground(UiStyle.COLOR_LABEL_TEXT);
        lblFont.setPreferredSize(new Dimension(220, 30));

        String[] fontOptions = {"맑은 고딕", "나눔고딕", "바탕체", "돋움", "굴림"};
        JComboBox<String> comboFont = new JComboBox<>(fontOptions){
            @Override
            protected void paintComponent(Graphics g){
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(borderGray);
                g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 10, 10);

                g2.dispose();
            }
        };
        comboFont.setSelectedItem(tempFont[0]);
        comboFont.setFont(UiStyle.FONT_PLAIN_12);
        comboFont.setPreferredSize(new Dimension(160, 30));
        comboFont.setOpaque(false);
        comboFont.setBackground(Color.WHITE);
        comboFont.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
        comboFont.setFocusable(false);
        comboFont.setUI(new javax.swing.plaf.basic.BasicComboBoxUI(){
            @Override
            protected JButton createArrowButton(){
                JButton button = new JButton(){
                    @Override
                    protected void paintComponent(Graphics g){
                        Graphics2D g2= (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.setColor(Color.GRAY);

                        g2.drawLine(5, 13, 9, 17);
                        g2.drawLine(9, 17, 13, 13);
                        g2.dispose();
                    }
                };
                button.setBorderPainted(false);
                button.setContentAreaFilled(false);
                button.setOpaque(false);
                return button;
            }
        });

        comboFont.setRenderer(new javax.swing.DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus){
                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                c.setBackground(Color.WHITE);
                c.setForeground(new Color(60, 65, 75));
                c.setFont(UiStyle.FONT_PLAIN_12);
                c.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return c;
            }
        });

        SpinnerModel spinnerModel = new SpinnerNumberModel(tempFontSize[0], 4, 40, 2);
        JSpinner spinnerSize = new JSpinner(spinnerModel);
        spinnerSize.setFont(UiStyle.FONT_BOLD_12);
        spinnerSize.setPreferredSize(new Dimension(65, 30));
        spinnerSize.setBorder(BorderFactory.createLineBorder(borderGray, 1, true));

        comboFont.addActionListener(e -> {
            tempFont[0] = (String) comboFont.getSelectedItem();
        });
        spinnerSize.addChangeListener(e -> {
            tempFontSize[0] = (int) spinnerSize.getValue();
        });

        rowFont.add(lblFont);
        rowFont.add(comboFont);

        rowFont.add(spinnerSize);

        baseSettingCard.add(rowFont);
        tabGeneral.add(baseSettingCard);
        tabGeneral.add(Box.createVerticalStrut(18));

        //[플랫폼 관리] 구역
        // (3) 플랫폼 추가 관리 행 조립
        JPanel platformBlockPanel = new JPanel(new GridBagLayout()){
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(new Color(230, 235, 240));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        platformBlockPanel.setBackground(Color.WHITE);
        platformBlockPanel.setOpaque(false);
        platformBlockPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        //최대 크기 조절
        platformBlockPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 200));
        platformBlockPanel.setMinimumSize(new Dimension(10, 200));
        platformBlockPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        platformBlockPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        //카드 내부에 소제목 배치 처리를 수동으로 조립하기 위해 GridBagConstraints 구조물 새로 연동
        GridBagConstraints gbcCard = new GridBagConstraints();
        gbcCard.fill = GridBagConstraints.BOTH;

        JLabel lblPlatTitle = new JLabel("플랫폼 관리");
        lblPlatTitle.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblPlatTitle.setForeground(themeCyan);
        gbcCard.gridx = 0;
        gbcCard.gridy = 0;
        gbcCard.gridwidth = 3;
        gbcCard.weightx = 1.0;
        gbcCard.weighty = 0.0;
        gbcCard.insets = new Insets(0, 0, 14, 0);
        platformBlockPanel.add(lblPlatTitle, gbcCard);

        //좌측 입력 폼 패널 배치 조립
        JPanel inputFormPanel = new JPanel();
        inputFormPanel.setBackground(Color.WHITE);
        inputFormPanel.setLayout(new BoxLayout(inputFormPanel, BoxLayout.Y_AXIS));

        inputFormPanel.setPreferredSize(new Dimension(410, 68));
        inputFormPanel.setMinimumSize(new Dimension(410, 68));
        inputFormPanel.setMaximumSize(new Dimension(410, 68));

        // 행 1: 플랫폼 이름 구역
        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        nameRow.setBackground(Color.WHITE);
        nameRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblPlatformName = new JLabel("플랫폼 이름");
        lblPlatformName.setFont(UiStyle.FONT_PLAIN_13);
        lblPlatformName.setForeground(new Color(60, 65, 70));
        lblPlatformName.setPreferredSize(new Dimension(140, 30));

        JTextField tfNewPlatform = new JTextField(){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(borderGray);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tfNewPlatform.setOpaque(false);
        tfNewPlatform.setFont(UiStyle.FONT_PLAIN_12);
        tfNewPlatform.setPreferredSize(new Dimension(260, 30));
        tfNewPlatform.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        nameRow.add(lblPlatformName);
        nameRow.add(tfNewPlatform);
        inputFormPanel.add(nameRow);
        inputFormPanel.add(Box.createVerticalStrut(2));        //행간 간격 격리

        // 행 2: 플랫폼 로고 파일 탐색 구역
        JPanel iconRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        iconRow.setBackground(Color.WHITE);
        iconRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblPlatformIcon = new JLabel("플랫폼 로고 이미지");
        lblPlatformIcon.setFont(UiStyle.FONT_PLAIN_13);
        lblPlatformIcon.setForeground(new Color(60, 65, 70));
        lblPlatformIcon.setPreferredSize(new Dimension(140, 30));

        JTextField tfIconPath = new JTextField(){
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(245, 246, 248)); // 읽기 전용 상태에 부합하는 소프트 그레이 채우기
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(borderGray);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tfIconPath.setOpaque(false);
        tfIconPath.setEditable(false);
        tfIconPath.setFont(UiStyle.FONT_PLAIN_11);
        tfIconPath.setPreferredSize(new Dimension(200, 30));
        tfIconPath.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        JButton btnBrowseIcon = new JButton("찾기"){
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(borderGray);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnBrowseIcon.setFont(UiStyle.FONT_BOLD_12);
        btnBrowseIcon.setForeground(new Color(70, 80, 90));
        btnBrowseIcon.setContentAreaFilled(false);
        btnBrowseIcon.setBorderPainted(false);
        btnBrowseIcon.setPreferredSize(new Dimension(60, 30));
        btnBrowseIcon.setFocusPainted(false);
        btnBrowseIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));

        iconRow.add(lblPlatformIcon);
        iconRow.add(tfIconPath);
        iconRow.add(btnBrowseIcon);
        inputFormPanel.add(iconRow);

        //중앙 우측: 실시간 로고 미리보기 컴포넌트 상자 개설
        JLabel lblPreview = new JLabel("미리보기", SwingConstants.CENTER){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);

                g2.setColor(new Color(230, 232, 235));  //테두리 컬러 칩
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);  //모서리 반경 8픽셀 라운딩 선 그리기

                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblPreview.setFont(UiStyle.FONT_PLAIN_11);
        lblPreview.setForeground(Color.LIGHT_GRAY);

        //미리보기 박스 크기
        lblPreview.setPreferredSize(new Dimension(240, 110));
        lblPreview.setMinimumSize(new Dimension(240, 110));
        lblPreview.setMaximumSize(new Dimension(240, 110));

        //극우측: 기능 조작 버튼 컬렉션 패널
        JPanel actionButtonPanel = new JPanel();
        actionButtonPanel.setBackground(Color.WHITE);
        actionButtonPanel.setLayout(new BoxLayout(actionButtonPanel, BoxLayout.Y_AXIS));

        JButton btnAddPlatform = new JButton("플랫폼 등록"){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                //[비즈니스 분기 주입]: 버튼이 활성화 상태면 정품 블루색, 잠긴 상태면 회색
                if(isEnabled()){
                    g2.setColor(new Color(0, 120, 215));
                } else{
                    g2.setColor(new Color(204, 204, 204));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);  //버튼 모서리 곡률 반지름 8픽셀 처리
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnAddPlatform.setFont(UiStyle.FONT_BOLD_12);
        btnAddPlatform.setForeground(Color.GRAY);
        btnAddPlatform.setFocusPainted(false);
        btnAddPlatform.setBorderPainted(false);     //외곽의 날카로운 기본 사각 테두리선 제거
        btnAddPlatform.setContentAreaFilled(false); //Swing 본래의  각진 컴포넌트 렌더링을 완전히 끔

        btnAddPlatform.setPreferredSize(new Dimension(110, 32));
        btnAddPlatform.setMaximumSize(new Dimension(110, 32));
        btnAddPlatform.setEnabled(false);       //초기 상태는 규칙에 의거해 강제 잠금 비활성화

        JButton btnDeletePlatform = new JButton("삭제"){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 242, 242));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(245, 190, 190));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnDeletePlatform.setFont(UiStyle.FONT_BOLD_12);
        btnDeletePlatform.setForeground(new Color(210, 50, 50));
        btnDeletePlatform.setFocusPainted(false);
        btnDeletePlatform.setBorderPainted(false);
        btnDeletePlatform.setContentAreaFilled(false);

        btnDeletePlatform.setPreferredSize(new Dimension(110, 32));
        btnDeletePlatform.setMaximumSize(new Dimension(110, 32));
        btnDeletePlatform.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnDeletePlatform.addActionListener(e -> openDeletePlatformDialog());

        actionButtonPanel.add(btnAddPlatform);
        actionButtonPanel.add(Box.createVerticalStrut(4));
        actionButtonPanel.add(btnDeletePlatform);

        //[하단 가이드라인 라벨 신설]
        JLabel lblGuideNotice = new JLabel("※ 이름과 로고 이미지를 선택해야 등록할 수 있습니다.");
        lblGuideNotice.setFont(UiStyle.FONT_PLAIN_11);
        lblGuideNotice.setForeground(UiStyle.COLOR_ICON_INACTIVE);

        //GridBagLayout을 통한 요소들의 비율 도킹 공간 연산
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;

        //1. 좌측 입력 폼 패널 배치
        gbc.insets = new Insets(0, 0, 0, 15);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        platformBlockPanel.add(inputFormPanel, gbc);

        // 2. 중앙 미리보기 칸 배치
        gbc.insets = new Insets(0, 0, 0, 15);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        platformBlockPanel.add(lblPreview, gbc);

        // 3. 우측 기능 조작 버튼 패널 배치
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        platformBlockPanel.add(actionButtonPanel, gbc);

        //하단 안내 라벨은 블록의 밑바닥 전체를 차지하도록 가로 병학(gridwidth) 세팅
        gbc.insets = new Insets(6, 2, 0, 0);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        platformBlockPanel.add(lblGuideNotice, gbc);

        //기팩 패널 흐름에 정식 안착
        tabGeneral.add(platformBlockPanel);

        //상호작용 엔진 비즈니스 로직 조립 구역
        //[검증 리스너]: 텍스트 변경을 실시간 감시하여 버튼 활성화 및 안내문구 제어
        Runnable validateFields = () -> {
            String nameText = tfNewPlatform.getText().trim();
            String pathText = tfIconPath.getText().trim();
            boolean isReady = !nameText.isEmpty() && !pathText.isEmpty();

            btnAddPlatform.setEnabled(isReady);
            if(isReady){
                btnAddPlatform.setForeground(Color.WHITE);
                btnAddPlatform.setCursor(new Cursor(Cursor.HAND_CURSOR));
                lblGuideNotice.setText("등록 공정을 시작할 준비가 완료되었습니다.");
                lblGuideNotice.setForeground(new Color(40, 167, 69));   //만족 시 녹색 처리
            } else{
                btnAddPlatform.setForeground(Color.GRAY);
                btnAddPlatform.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                lblGuideNotice.setText("이름과 로고 이미지를 선택해야 등록할 수 있습니다.");
                lblGuideNotice.setForeground(UiStyle.COLOR_ICON_INACTIVE);
            }
        };

        tfNewPlatform.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { validateFields.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { validateFields.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { validateFields.run(); }
        });

        //[찾기 버튼 액션]: 이미지 획득 시 즉시 미리보기 칸에 리사이징 및 인쇄 가동
        btnBrowseIcon.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            javax.swing.filechooser.FileNameExtensionFilter filter =
                    new javax.swing.filechooser.FileNameExtensionFilter("이미지 파일 (*.png, *.jpg)", "png", "jpg", "jpeg");
            chooser.setFileFilter(filter);

            int result = chooser.showOpenDialog(mainFrame);
            if(result == JFileChooser.APPROVE_OPTION){
                String imgPath = chooser.getSelectedFile().getAbsolutePath();
                tfIconPath.setText(imgPath);

                //미리보기 이미지 고화질 렌더링
                try{
                    ImageIcon icon = new ImageIcon(imgPath);
                    //가로 64, 세로 64 규격에 맞춰 종횡비 스케일링 가동
                    Image scaledImg = icon.getImage().getScaledInstance(72, 72, Image.SCALE_SMOOTH);
                    lblPreview.setIcon(new ImageIcon(scaledImg));
                    lblPreview.setText(""); //기존 기본 안내 텍스트 소거
                } catch(Exception ex){
                    lblPreview.setText("ERR");
                }
                validateFields.run();   //파일 경로 획득 후 즉시 검증 엔진 작동
            }
        });

        //[등록 실행 버튼]: 단일 통합 수집 로직
        btnAddPlatform.addActionListener(evt -> {
            String pName = tfNewPlatform.getText().trim();
            String iPath = tfIconPath.getText().trim();

            if(!pName.isEmpty()){
                AppSettings.getInstance().addPlatformWithIcon(pName, iPath);

                JOptionPane.showMessageDialog(mainFrame, "[" + pName +
                        "] 플랫폼 및 로고 이미지 연동 세트 등록이 성공적으로 완료되었습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                tfNewPlatform.setText("");
                tfIconPath.setText("");
                lblPreview.setIcon(null);
                lblPreview.setText("미리보기");
                validateFields.run();   //등록 완료 후 폼 청소 및 리락킹

                java.awt.event.ActionListener tempListener = platformCombo.getActionListeners()[0];
                platformCombo.removeActionListener(tempListener);

                Object selected = platformCombo.getSelectedItem();
                platformCombo.removeAllItems();
                platformCombo.addItem("전체 플랫폼");
                for(String p : AppSettings.getInstance().getCustomPlatforms()){
                    platformCombo.addItem(p);
                }
                platformCombo.setSelectedItem(selected);
                platformCombo.addActionListener(tempListener);
            }
        });


        //일반 설정 배치 스트림에 조립 장착
        tabGeneral.add(Box.createVerticalGlue());

        JPanel rowFinalAction = new JPanel(new BorderLayout());
        rowFinalAction.setBackground(Color.WHITE);
        rowFinalAction.setAlignmentX(Component.LEFT_ALIGNMENT);
        //가로 폭이 컴포넌트 배치에 따라 뒤틀리지 않도록 최대 사이즈 가이드 락 고정
        rowFinalAction.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));


        //[좌측 최하단]: 버튼 형태 탈피, 텍스트 링크 형태의 초기화 컴포넌트
        JButton btnClearLink = new JButton("전체 데이터 초기화");
        btnClearLink.setFont(UiStyle.FONT_PLAIN_12);
        btnClearLink.setForeground(themeCyan);
        btnClearLink.setFocusPainted(false);
        btnClearLink.setBorderPainted(false);
        btnClearLink.setContentAreaFilled(false);
        btnClearLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClearLink.setPreferredSize(new Dimension(140, 32));

        btnClearLink.setUI(new javax.swing.plaf.basic.BasicButtonUI(){
            private boolean isHover = false;
            @Override
            protected void installListeners(AbstractButton b){
                super.installListeners(b);
                b.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHover = true;
                        b.setForeground(Color.RED);
                        b.repaint();
                    }
                    @Override
                    public void mouseExited(MouseEvent e){
                        isHover = false;
                        b.setForeground(themeCyan);
                        b.repaint();
                    }
                });
            }
            @Override
            public void paint(Graphics g, JComponent c){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(isHover ? new Color(255, 240, 240) : Color.WHITE);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 6, 6);

                g2.setColor(isHover ? Color.RED : new Color(218, 224, 230));
                g2.drawRoundRect(0, 0, c.getWidth()- 1, c.getHeight()-1, 6, 6);

                g2.dispose();
                super.paint(g, c);
            }
        });

        //데이터 마스터 초기화 비지니스 로직 연동
        btnClearLink.addActionListener(e -> {

            // 1. 서재 선택 팝업 창 생성
            JDialog selectDialog = new JDialog(mainFrame, "초기화할 서재 선택", true);
            selectDialog.setLayout(new GridLayout(3, 1, 10, 10));
            selectDialog.setSize(300, 200);
            selectDialog.setLocationRelativeTo(mainFrame);
            selectDialog.getContentPane().setBackground(Color.WHITE);

            JLabel lblMsg = new JLabel("어떤 서재의 데이터를 초기화하시겠습니까?", SwingConstants.CENTER);
            lblMsg.setFont(UiStyle.FONT_BOLD_12);
            selectDialog.add(lblMsg);

            JButton btnDelMy = new JButton("내 서재");
            JButton btnDelParody = new JButton("패러디 서재");

            // 초기화 공통 로직
            java.util.function.Consumer<String> performReset = (type) -> {
                int confirm = JOptionPane.showConfirmDialog(selectDialog,
                        type + "의 프로그램 내 기록을 삭제하시겠습니까?\n(실제 소설 텍스트 파일은 안전하게 보존됩니다)",
                        "확인", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                if (confirm == JOptionPane.YES_OPTION) {
                    if (type.equals("내 서재")) {
                        novelList.clear();
                        saveLibraryData();
                        refreshLibrary();
                    } else if (type.equals("패러디 서재")) {
                        new File("C:\\novel\\parody_data.txt").delete();
                        if(parodyPanel != null) parodyPanel.clearAllData();
                    }
                    JOptionPane.showMessageDialog(selectDialog, type + " 데이터가 초기화되었습니다.");
                    selectDialog.dispose();
                }
            };

            btnDelMy.addActionListener(ev -> performReset.accept("내 서재"));
            btnDelParody.addActionListener(ev -> performReset.accept("패러디 서재"));

            selectDialog.add(btnDelMy);
            selectDialog.add(btnDelParody);

            selectDialog.setVisible(true);
        });

        //[우측 최하단]: 설정 적용 버튼
        JButton btnApplyFinal = new JButton("설정 적용"){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                //배경색 칠하기
                g2.setColor(UiStyle.COLOR_ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);  //모서리 곡률 반지름 8
                g2.dispose();

                super.paintComponent(g);
            }
        };

        btnApplyFinal.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        btnApplyFinal.setForeground(Color.WHITE);
        btnApplyFinal.setFocusPainted(false);
        btnApplyFinal.setBorderPainted(false);  //사각 테두리선 강제 거세
        btnApplyFinal.setContentAreaFilled(false);  //자바 Swing의 기본 사각 배경 인쇄 기능 비활성화
        btnApplyFinal.setPreferredSize(new Dimension(140, 36));
        btnApplyFinal.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnApplyFinal.addActionListener(e -> {
            AppSettings.getInstance().setDefaultTheme(tempTheme[0]);
            AppSettings.getInstance().setDefaultFontName(tempFont[0]);
            AppSettings.getInstance().setDefaultFontSize(tempFontSize[0]);
            AppSettings.getInstance().saveSettings();   //설정 저장 명시 누락 보정

            JOptionPane.showMessageDialog(mainFrame, "모든 환경 설정 사양이 성공적으로 저장 및 반영되었습니다.",
                    "알림", JOptionPane.INFORMATION_MESSAGE);
        });

        //신설된 최종 행 패널의 동서벽면에 컴포넌트 도킹
        rowFinalAction.add(btnClearLink, BorderLayout.WEST);
        rowFinalAction.add(btnApplyFinal, BorderLayout.EAST);

        //일반 설정 탭 본문 최하단에 레이아웃 최종 배치
        tabGeneral.add(rowFinalAction);


        //종합 패킹 및 뷰 포트 화면 출력 발사]
        tabbedPane.addTab("일반 설정", tabGeneral);

        settingsPanel.add(tabbedPane, BorderLayout.CENTER);
        settingsPanel.revalidate();
        settingsPanel.repaint();
    }

    //[핵심 필터링/정렬 메소드] : 상단 조작 상태에 따라 화면을 실시간 클리닝 및 동적 카드 재구성
    // + 외부 클래스에서 제어할 수 있도록 접근 권한을 public으로 바꿈
    public void refreshLibrary(){

        if("패러디 서재".equals(this.currentMenu) && parodyPanel != null){
            parodyPanel.refreshParodyLibrary();
        }
        if(libraryGridPanel == null) return;

        //상단 컨트롤러들의 현재 선태 상태 값 출력
        String selectedPlatform = (platformCombo != null) ? (String) platformCombo.getSelectedItem() : "전체 플랫폼";
        String selectedSort = (sortCombo != null) ? (String) sortCombo.getSelectedItem() : "최근 읽은 순";
        String searchText = (searchField != null) ? searchField.getText() : "";

        //화면 청소 및 초기화
        libraryGridPanel.removeAll();
        cardCount = 0;

        //1. 기존 조건(플랫폼, 검색어, 좌측 메뉴)을 먼저 통과한 기초 데이터를 수집하고 산출
        ArrayList<Novel> baseFilteredList = new ArrayList<>();
        int countAll = 0, countOngoing = 0, countCompleted = 0, countHiatus = 0;

        for(Novel novel : novelList){
            //[A] 플랫폼 필터 검사
            boolean matchesPlatform = selectedPlatform.equals("전체 플랫폼") || novel.getPlatform().contains(selectedPlatform);

            //[B] 다중 해시태크(#) 교차 검색 알고리즘
            boolean matchesSearch = true;
            String userQuery = searchText.trim().toLowerCase();

            if(!userQuery.isEmpty() && !userQuery.equals("제목, 작가, 태그 검색...")){
                // case1 : 샵(#) 기호가 있으면 [키워드 검색 모드] 가동
                if(userQuery.contains("#")){
                    String[] tokens = userQuery.split("\\s+");      //공백 단위로 먼저 분할
                    String novelKeywords = novel.getKeywords() != null ? novel.getKeywords().toLowerCase() : "";
                    for(String token : tokens){
                        if(token.startsWith("#") && token.length() > 1){
                            String cleanKey = token.substring(1).replace(",", "").trim();   //'#'기호 제거 및 쉼표 제거
                            if(!cleanKey.isEmpty() && !novelKeywords.contains(cleanKey)){
                                matchesSearch = false;      //하나라도 포함 안 되면 탈락(AND 연산)
                                break;
                            }
                        }
                    }
                } // Case2: 골뱅이(@) 기호가 있으면 [작가 검색 모드] 가동
                else if(userQuery.startsWith("@")){
                    String cleanAuthor = userQuery.substring(1).trim(); //'@'기호 제거
                    String novelAuthor = novel.getAuthor() != null ? novel.getAuthor().toLowerCase() : "";

                    //일부 글자만 입력해도 검색 결과가 나오도록 설정
                    if(!cleanAuthor.isEmpty() && !novelAuthor.contains(cleanAuthor)) {
                        matchesSearch = false;
                    }
                }
                // Case3: 아무 기호도 없으면 [작품 제목 검색 모드] 작동
                else{
                    //일부 글자만 입력해도 부분 일치로 검색되도록 처리
                    matchesSearch = novel.getTitle().toLowerCase().contains(userQuery);
                }
            }

            //[C] 좌측 메뉴 탭 필터 검사
            //선택된 메뉴가 '좋아요'일 때는 즐겨찾기(isFavorite)가 true인 작품만 통과
            boolean matchesMenu = true;
            if(currentMenu.equals("좋아요")){
                matchesMenu = novel.isFavorite();
            }
            else if(currentMenu.equals("내 서재") && selectedSort.equals("안 읽은 작품")){
                //안 읽은 작품 필터 정의: 마지막 열람일 데이터가 "기록 없음" 또는 "완독하지 못한 소설"
                if(novel.getLastReadDate().equals("기록 없음")){
                    //아예 손대지 않은 소설은 무조건 노출 대상에 포함
                    matchesMenu = true;
                }
                else{
                    //읽은 기록이 있다면 실제 폴더 스캔을 통해 남은 회차가 있는지 확인
                    int totalCh = 0;
                    int currentCh = 1;

                    File dir = new File(novel.getFolderPath());
                    if(dir.exists() && dir.isDirectory()){
                        File[] files = dir.listFiles((d, name) -> {
                            String lower = name.toLowerCase();
                            return lower.endsWith(".txt")
                                    && !lower.equals("bookmark.txt")
                                    && !lower.equals("memo_bookmarks.txt")
                                    && !lower.startsWith("summary_notes");
                        });
                        if(files != null) totalCh = files.length;
                    }

                    File bookmarkFile = new File(novel.getFolderPath() + File.separator + "bookmark.txt");
                    if(bookmarkFile.exists()){
                        try(BufferedReader br = new BufferedReader(new FileReader(bookmarkFile))){
                            String line = br.readLine();
                            if(line != null) currentCh = Integer.parseInt(line.trim());
                        } catch (Exception e) {}
                    }
                    //현재 읽은 위치(currentCh)가 총 파일 수(totalCh)보다 '덜 읽은 작품'만 필터망에 통과
                    matchesMenu = (currentCh < totalCh);
                }
            }

            // 1차 필터망 통과 객체 수집 및 탭 전용 카운팅
            if(matchesPlatform && matchesSearch && matchesMenu){
                baseFilteredList.add(novel);
                countAll++;
                if(novel.isCompleted()) countCompleted++;
                else if(novel.isHiatus()) countHiatus++;
                else countOngoing++;
            }
        }

        // 2. 탭UI 텍스트에 실시간 통계 수치 반영
        if(btnTabAll != null) btnTabAll.setText("전체 (" + countAll + ")");
        if(btnTabOngoing != null) btnTabOngoing.setText("연재중 (" + countOngoing + ")");
        if(btnTabCompleted != null) btnTabCompleted.setText("완결 (" + countCompleted + ")");
        if(btnTabHiatus != null) btnTabHiatus.setText("연재중단 (" + countHiatus + ")");

        // 3. 현재 선택된 탭 상태를 기준으로 2차 최종 필터링
        ArrayList<Novel> finalFilteredList = new ArrayList<>();
        for(Novel novel : baseFilteredList) {
            boolean matchesTab = true;
            if (currentStatusTab.equals("완결")) matchesTab = novel.isCompleted();
            else if (currentStatusTab.equals("연재중단")) matchesTab = novel.isHiatus();
            else if (currentStatusTab.equals("연재중")) matchesTab = !novel.isCompleted() && !novel.isHiatus();

            if (matchesTab) finalFilteredList.add(novel);
        }
        // 4. 좌측 상단 총 카운터 라벨 동기화
        if(lblTotalCounter != null){
            lblTotalCounter.setText("작품 수: " + finalFilteredList.size() + "개");
        }

        // 5. 정렬 알고리즘
        if(selectedSort.equals("좋아요 순")){
            Collections.sort(finalFilteredList, (n1, n2) -> Long.compare(n2.getFavoriteTimestamp(), n1.getFavoriteTimestamp()));
        } else if(selectedSort.equals("최근 읽은 순") || selectedSort.equals("안 읽은 작품")){
            Collections.sort(finalFilteredList, (n1, n2) -> {
                String d1 = n1.getLastReadDate();
                String d2 = n2.getLastReadDate();

                // +읽지 않아서 날짜 기록이 없는 작품은 가중치를 -1과 1로 반전하여 리스트의 가장 최하단으로 자동 낙하 처리
                if(d1.equals("기록 없음") && d2.equals("기록 없음")) return 0;
                if(d1.equals("기록 없음")) return 1;
                if(d2.equals("기록 없음")) return -1;
                return d2.compareTo(d1);    //최신 날짜가 앞으로 오도록 매핑
            });
        }
        else if(selectedSort.equals("제목순")){
            Collections.sort(finalFilteredList, (n1, n2) -> n1.getTitle().compareTo(n2.getTitle()));
        }

        //6. 정제 완료된 소설 카드들만 가나다/최신순/미완독 정렬에 매핑하여 서재에 노출
        for(Novel novel : finalFilteredList){
            addNovelCard(novel);
        }

        libraryGridPanel.revalidate();
        libraryGridPanel.repaint();
    }

    //Novel 데이터를 기반으로 화면에 카드를 그리는 메서드
    private void addNovelCard(Novel novel){
        // 1. 카드 배경 투명화 및 외곽선 제거
        JPanel card = new JPanel(){
            @Override
            protected void paintComponent(Graphics g){
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                //그래픽 연산 화질 최상으로 설정(앤티앨리어싱 활성화)
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int cornerRadius = 16;  //모서리 곡률 반경 세팅

                //1. 은은한 그림자 드로잉 연산(바깥으로 번지는 이중 레이어 음영)
                g2.setColor(new Color(0, 0, 0, 4));     //극도로 투명한 블랙으로 외곽 블러 표현
                g2.fillRoundRect(1, 1, w-2, h-2, cornerRadius, cornerRadius);
                g2.setColor(new Color(0, 0, 0, 10));    //살짝 짙은 경계선 음영
                g2.fillRoundRect(2, 2, w-4, h-4, cornerRadius, cornerRadius);

                //2. 카드 본체 둥근 하얀색 바닥면 페인팅
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(2, 2, w-5, h-5, cornerRadius, cornerRadius);

                //3. 아주 미세하고 깨끗한 외곽 그레이 가이드라인 마감
                g2.setColor(new Color(235, 238, 242));
                g2.drawRoundRect(2, 2, w-5, h-5, cornerRadius, cornerRadius);

                g2.dispose();
            }
        };

        //투명도 속성을 켜줘야 paintComponent로 그린 라운딩 바깥 영역이 부모 배경과 투명과 동기화됨
        card.setOpaque(false);
        card.setBackground(new Color(0, 0, 0, 0));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        // 카드 규격
        card.setPreferredSize(new Dimension(190, 325));
        card.setMaximumSize(new Dimension(190, 325));   //카드 전체 컨테이너 규격 고정

        //마우스 커서를 손가락 모양으로 바꾸어 클릭 가능한 느낌 주기
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        //2. 이미지 표시 및 그래픽스 스케일링 엔진
        int targetWidth = 166;
        int targetHeight = 170;

        JLabel lblCover = new JLabel(){
            @Override
            protected void paintComponent(Graphics g){
                //만약 실제 커버 이미지가 등록되어 있지 않은 상태라면 커스텀 박스를 드로잉
                if(novel.getCoverPath().isEmpty() || !new File(novel.getCoverPath()).exists()){
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    //1.바탕 연회색 라운드 컨테이너 채우기
                    g2.setColor(new Color(245, 247, 250));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                    //2. 미니멀 책 모양 아이콘 정밀 드로잉
                    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.setColor(new Color(160, 170, 185));

                    int cx = getWidth() / 2;
                    int cy = getHeight() /2 - 18;

                    //왼쪽 페이지 곡선 및 수직벽 드로잉
                    g2.drawArc(cx - 11, cy - 6, 11, 6, 0, 180); //상단 곡선
                    g2.drawLine(cx-11, cy-3, cx-11, 6+cy);                          //외곽 수직선
                    g2.drawLine(cx, cy-3, cx,6+cy);                                         //중앙 수직선
                    g2.drawArc(cx-11, cy+3, 11, 6, 0, 180);    //하단 곡선

                    //오른쪽 페이지 곡선 및 수직벽 드로잉
                    g2.drawArc(cx, cy - 6, 11, 6, 0, 180);      //상단 곡선
                    g2.drawLine(cx + 11, cy - 3, cx + 11, 6 + cy);                //외곽 수직선
                    g2.drawArc(cx, cy+3, 11, 6, 0, 180);      //하단 곡선

                    //중앙 책갈피 코어 기둥선
                    g2.drawLine(cx, cy-6, cx, 6+cy);

                    g2.dispose();
                }
                super.paintComponent(g);

                //표지 좌측 상단에 상태 오버레이 배지 드로잉(완결/연재중단)
                if(novel.isCompleted() || novel.isHiatus()){
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    boolean isComp = novel.isCompleted();
                    String badgeText = isComp ? "완결" : "연재중단";
                    Color badgeColor = isComp ? new Color(0, 160, 120) : new Color(255, 120, 0);
                    int badgeWidth = isComp ? 44 : 60;

                    //그림자 효과
                    g2.setColor(new Color(0, 0, 0, 30));
                    g2.fillRoundRect(9, 9, badgeWidth, 22, 6, 6);

                    //배지 배경
                    g2.setColor(badgeColor);
                    g2.fillRoundRect(8, 8, badgeWidth, 22, 6, 6);

                    //배지 텍스트 렌더링 및 중앙 정렬
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("맑은 고딕", Font.BOLD, 11));

                    FontMetrics fm = g2.getFontMetrics();
                    int textX = 8 + (badgeWidth - fm.stringWidth(badgeText)) / 2;
                    int textY = 8 + ((22 - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(badgeText, textX, textY);

                    g2.dispose();
                }
            }
        };
        lblCover.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblCover.setHorizontalAlignment(SwingConstants.CENTER);
        lblCover.setPreferredSize(new Dimension(targetWidth, targetHeight));
        lblCover.setMaximumSize(new Dimension(targetWidth, targetHeight));

        if(!novel.getCoverPath().isEmpty() && new File(novel.getCoverPath()).exists()){
            try{
                File coverFile = new File(novel.getCoverPath());
                //파일 경로+수정시각을 키로 사용 → 표지 파일이 바뀌면 자동으로 캐시 무효화됨
                String cacheKey = coverFile.getAbsolutePath() + "_" + coverFile.lastModified();
                ImageIcon cached = coverImageCache.get(cacheKey);

                if(cached != null){
                    lblCover.setIcon(cached);
                } else {
                    java.awt.image.BufferedImage srcImg = javax.imageio.ImageIO.read(coverFile);
                    int srcWidth = srcImg.getWidth(null);
                    int srcHeight = srcImg.getHeight(null);

                    double targetScale = Math.max((double) targetWidth / srcWidth, (double) targetHeight / srcHeight);
                    int scaledWidth = (int) (srcWidth * targetScale);
                    int scaledHeight = (int) (srcHeight * targetScale);

                    //중앙 정렬을 위한 잘라내기 시작 좌표 도출
                    int x = (targetWidth - scaledWidth) / 2;
                    int y = (targetHeight - scaledHeight) / 2;

                    //3. 고화질 캔버스 준비 및 그래픽스 엔진 드로잉
                    java.awt.image.BufferedImage resizedImg = new java.awt.image.BufferedImage(
                            targetWidth, targetHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB
                    );
                    Graphics2D g2 = resizedImg.createGraphics();

                    //초고화질 그래픽 렌더링 힌트 다중 주입
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // 표지 상단 모서리만 둥글게 클리핑 처리(하단은 그라데이션으로 덮이므로 여유 마진 부여)
                    g2.setClip(new RoundRectangle2D.Float(0, 0, targetWidth, targetHeight, 14, 14));
                    g2.drawImage(srcImg, x, y, scaledWidth, scaledHeight, null);

                    // 표지 하단에 자연스러운 그라데이션 페이드아웃 효과 적용
                    GradientPaint fade = new GradientPaint(
                            0, targetHeight - 60, new Color(255, 255, 255, 0),  // 하단 60px 위부터 투명하게 시작
                            0, targetHeight, Color.WHITE    //맨 밑바닥은 완전한 불투명 흰색
                    );
                    g2.setPaint(fade);
                    g2.fillRect(0, targetHeight - 60, targetWidth, 60);

                    g2.dispose();

                    lblCover.setIcon(new ImageIcon(resizedImg));
                    ImageIcon newIcon = new ImageIcon(resizedImg);
                    coverImageCache.put(cacheKey, newIcon);
                    lblCover.setIcon(newIcon);
                }
            } catch(Exception e){
                lblCover.setText("IMAGE ERROR");
            }
        }

        //이미지 상단 여백 제거
        JPanel coverWrapper = new JPanel(new BorderLayout());
        coverWrapper.setOpaque(false);
        coverWrapper.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        coverWrapper.setPreferredSize(new Dimension(190, targetHeight + 12));
        coverWrapper.setMaximumSize(new Dimension(190, targetHeight + 12));   //최대 높이 완화
        coverWrapper.add(lblCover, BorderLayout.CENTER);
        coverWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        //2. 텍스트 정보 표시 구역(제목, 작가, 플랫폼)
        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12)); //텍스트 좌우 여백 확보
        infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 다중제목 <html> 태그를 활용하여 자동 줄바꿈
        String displayTitle = novel.getTitle();
        JLabel lblTitle = new JLabel("<html><body style='width: 160px; word-wrap: break-word; margin: 0; padding: 0; line-height: 1.1;'>" + displayTitle + "</body></html>");
        lblTitle.setFont(UiStyle.FONT_BOLD_13);
        lblTitle.setForeground(new Color(30, 35, 40));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        //infoPanel이 세로 공간을 더 이상 임의로 흡수하여 여백을 벌리지 못하도록 최대 크기 고정 박스 처리
        infoPanel.setPreferredSize(new Dimension(190, 145));
        infoPanel.setMaximumSize(new Dimension(190, 145));

        //제목 구역 레이아웃 고도화
        JPanel titleContainerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0,0));
        titleContainerRow.setOpaque(false);
        titleContainerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        //생성한 제목 라벨을 패널에 부착하여 화면에 출력되도록 연동
        titleContainerRow.add(lblTitle);

        // 장르ㆍ작가이름 조합 출력
        String genreStr = (novel.getGenre() == null || novel.getGenre().isEmpty() ? "미분류" : novel.getGenre());
        String authorStr = (novel.getAuthor() == null || novel.getAuthor().isEmpty() ? "작자미상" : novel.getAuthor());
        JLabel lblGenreAuthor = new JLabel(genreStr + "ㆍ" + authorStr);
        lblGenreAuthor.setFont(UiStyle.FONT_PLAIN_11);

        lblGenreAuthor.setForeground(Color.LIGHT_GRAY);
        lblGenreAuthor.setAlignmentX(Component.LEFT_ALIGNMENT);

        //출신 플랫폼 로고 이미지
        JPanel platformImageContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        platformImageContainer.setOpaque(false);
        platformImageContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        //등록된 플랫폼 문자열 파싱(ex."네이버 시리즈, 카카오페이지");
        String originPlatformStr = novel.getPlatform();

        //각 플랫폼 키워드가 포함되어 있다면 대응하고 로고 이미지를 생서아형 컨테이너에 부착
        if(originPlatformStr.contains("카카오페이지")){
            platformImageContainer.add(createPlatformLogoLabel("C:\\novel\\icon\\kakao.png", "카카오페이지"));
        }
        if(originPlatformStr.contains("네이버 시리즈")){
            platformImageContainer.add(createPlatformLogoLabel("C:\\novel\\icon\\series.png", "네이버 시리즈"));
        }
        if(originPlatformStr.contains("조아라")){
            platformImageContainer.add(createPlatformLogoLabel("C:\\novel\\icon\\joara.png", "조아라"));
        }
        if(originPlatformStr.contains("문피아")){
            platformImageContainer.add(createPlatformLogoLabel("C:\\novel\\icon\\munpia.png", "문피아"));
        }

        //사용자가 동적으로 등록한 커스텀 플랫폼 어레이리스트를 역추적하여 카드에 매핑하는 코드를 주입
        ArrayList<String> customPlatforms = AppSettings.getInstance().getCustomPlatforms();
        ArrayList<String> customIcons = AppSettings.getInstance().getCustomPlatformIcons();

        //등록된 커스텀 플랫폼 배열을 순회하며 매칭 검사
        for(int i=0; i < customPlatforms.size(); i++){
            String cpName = customPlatforms.get(i);

            //시스템 기본 4대 플랫폼은 위의 하드코딩 구역과 중복 렌더링되지 않도록 패스 가드 처리
            if(cpName.equals("네이버 시리즈") || cpName.equals("카카오페이지") || cpName.equals("조아라") || cpName.equals("문피아")){
                continue;
            }

            //현재 소설의 플랫폼 필드에 커스텀 플랫폼명이 포함되어 있고, 동기화된 아이콘 경로가 유효하지 검증
            if(originPlatformStr.contains(cpName) || cpName.contains(originPlatformStr) && i < customIcons.size()){
                String cpIconPath = customIcons.get(i);
                //아이콘 플래그가 none이 아닐 때만 동적 로고 라벨 생성 부착 가동
                if(!cpIconPath.equals("none")){
                    platformImageContainer.add(createPlatformLogoLabel(cpIconPath, cpName));
                }
            }
        }

        //만약 기본 4대 플랫폼 외에 다른 플랫폼이거나 등록된 이미지가 없을 때를 대비한 텍스트 Fallback 가드 로직
        if(platformImageContainer.getComponentCount() == 0){
            JLabel lblFallbackPlatform = new JLabel("[" + originPlatformStr + "]");
            lblFallbackPlatform.setFont(new Font("맑은 고딕", Font.BOLD, 10));
            platformImageContainer.add(lblFallbackPlatform);
        }

        //1. 총 편수 및 마지막으로 본 편수를 계산하여 진척도(X/Y) 도출
        int totalCh = 0;
        int currentCh = 1;

        File dir = new File(novel.getFolderPath());
        if(dir.exists() && dir.isDirectory()){
            File[] files = dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".txt") && !name.contains("bookmark") && !name.startsWith("summary"));
            if(files != null) totalCh = files.length;
        }

        //실제 bookmark.txt값 파악
        File bookmarkFile = new File(novel.getFolderPath() + File.separator + "bookmark.txt");
        if(bookmarkFile.exists()){
            try(BufferedReader br = new BufferedReader(new FileReader(bookmarkFile))){
                String line = br.readLine();
                if(line != null) currentCh = Integer.parseInt(line.trim());
            } catch(Exception e){}
        }
        if(totalCh == 0) totalCh = 1;       //분모 예외 처리
        int percent = (int) ((double)currentCh / totalCh * 100);
        if(percent > 100) percent = 100;

        // 2. 상단 텍스트 정보( 읽는 중 X / Y화 Z% )
        JPanel progressTextRow = new JPanel(new BorderLayout());
        progressTextRow.setOpaque(false);
        progressTextRow.setMaximumSize(new Dimension(190, 20));
        progressTextRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        String progressStr = (currentCh >= totalCh) ? "완독 " + totalCh + "화" : "읽는 중 " + currentCh + " / " + totalCh + "화";
        JLabel lblProgressText = new JLabel(progressStr);
        lblProgressText.setFont(UiStyle.FONT_PLAIN_11);
        lblProgressText.setForeground(currentCh >= totalCh ? new Color(0, 160, 160) : new Color(120, 125, 130));

        JLabel lblPercent = new JLabel(percent + "%");
        lblPercent.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        lblPercent.setForeground(UiStyle.COLOR_ACCENT);

        progressTextRow.add(lblProgressText, BorderLayout.WEST);
        progressTextRow.add(lblPercent, BorderLayout.EAST);

        final int finalPercent = percent;

        // 3. 커스텀 게이지바 드로잉
        JPanel progressBar = new JPanel(){
            @Override
            protected void paintComponent(Graphics g){
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 배경 바(연회색)
                g2.setColor(new Color(230, 235, 240));
                g2.fillRoundRect(0, 0, getWidth(), 4, 4, 4);

                // 채원진 바 (청록색)
                int fillWidth = (int) (getWidth() * (finalPercent / 100.0));
                g2.setColor(new Color(0, 160, 160));
                g2.fillRoundRect(0, 0, fillWidth, 4, 4, 4);
                g2.dispose();
            }
        };
        progressBar.setOpaque(false);
        progressBar.setPreferredSize(new Dimension(190, 4));
        progressBar.setMaximumSize(new Dimension(190, 4));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        // infoPanel 조립부
        infoPanel.add(titleContainerRow);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(lblGenreAuthor);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(platformImageContainer);

        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(progressTextRow);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(progressBar);

        card.add(coverWrapper);
        card.add(infoPanel);


        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // 마우스 진입 시 배경색을 아주 미세하게 밝은 청록색으로 변경하여 활성 상태 강조
                card.setBackground(new Color(245, 252, 252));
                card.setOpaque(true);
                card.repaint(); // 배경 변경사항 즉시 렌더링
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 마우스 이탈 시 다시 투명한 상태로 복구
                card.setBackground(new Color(0, 0, 0, 0));
                card.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                NovelDetailPage detailPage = new NovelDetailPage();
                detailPage.openDetailPage(novel, BookShelfPage.this);
            }
        });

        //GirdBagLayout 속성을 조작하여 1줄에 3개씩 배치되도록 좌표 인자를 동적 매핑
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = cardCount % 4;      //열 좌표(0, 1, 2 순환)
        gbc.gridy = cardCount / 4;      //행 좌표(3개 찰 때마다 1씩 증가
        gbc.anchor = GridBagConstraints.NORTHWEST;  //왼쪽 상단 고정 정렬

        gbc.fill = GridBagConstraints.NONE;

        //사방 20픽셀 마진을 부여하여 카드 간 간격을 벌리고, 확장권을 주입해 정중앙 쏠림을 파괴
        gbc.insets = new Insets(14, 14, 14, 14);
        gbc.weighty = 1.0;

        //메인 서재 패널에 붙이고 화면 새로고침
        libraryGridPanel.add(card, gbc);
        cardCount++;    //등록 순서 카운터 누적

        libraryGridPanel.revalidate();
        libraryGridPanel.repaint();
    }

    //novelList의 모든 소설 정보를 library_data.txt. 파일에 저장하는 메서드
    public void saveLibraryData(){
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(LIBRARY_DATA_FILE))){
            for(Novel novel : novelList){
                //작품 소개 내용물에 줄바굼(\n)이 포함되어 있다면 파일 안전을 위해 [NEWLINE] 특수 키워드로 변환하여 저장
                String safeDescription = (novel.getDescription() != null) ? novel.getDescription().replace("\n", "[NEWLINE]") : "";

                //각 항목을 기호(|)로 연결하여 한 줄씩 저장
                String line = novel.getTitle() + "|" +
                        novel.getAuthor() + "|" +
                        novel.getGenre() + "|" +
                        novel.getPlatform() + "|" +
                        novel.getFolderPath() + "|" +
                        novel.getCoverPath() + "|" +
                        novel.getKeywords() + "|" +
                        safeDescription + "|" +
                        novel.getLastReadDate() + "|" +
                        novel.isFavorite() + "|" +
                        novel.isCompleted() + "|" +
                        novel.getCreatedDate() + "|" +
                        novel.isHiatus();
                bw.write(line);
                bw.newLine();
            }
        } catch(IOException e){
            System.out.println("보관함 데이터 저장 실패: " + e.getMessage());
        }

        if(parodyPanel != null){
            parodyPanel.saveParodyMetadata();
        }
    }

    //프로그램 시작 시 library_data.txt 파일을 읽어와 서재를 자동으로 복구하는 메서드
    private void loadLibraryData(){
        File file = new File(LIBRARY_DATA_FILE);
        if(!file.exists()) return;  //저장된 파일이 없으면 그냥 리턴

        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String line;
            //로딩 시 카운터를 리셋하여 동적 좌표가 깨지는 현상을 방지
            cardCount = 0;
            novelList.clear();      //중복 데이터 방지 초기화 리셋
            while((line = br.readLine()) != null){
                //기호(|)를 기준으로 문자열을 쪼갠다. 자바 문법상 특수기호(|)는 앞바바(\\|)처리해야함
                String[] data = line.split("\\|", -1);
                if(data.length >= 10){
                    //파일에서 읽어온 후 [NEWLINE] 기호를 자바용 엔터값(\n)으로 복원
                    String restoredDescription = data[7].replace("[NEWLINE]", "\n");

                    Novel novel = new Novel(data[0], data[1], data[2], data[3], data[4],
                            data[5], data[6], restoredDescription, data[8], Boolean.parseBoolean(data[9])
                    );

                    //파일 내 11번째 칸(data[10]에 완결 세이브 정보가 기록되어 있다면 이를 해석하여 주입
                    if(data.length >= 11){
                        novel.setCompleted(Boolean.parseBoolean(data[10]));
                    }
                    //12번째 칸(data[11])에 백업된 고유 등록일을 역추적 복원 주입
                    if(data.length >= 12){
                        novel.setCreatedDate(data[11]);
                    }
                    if(data.length >= 13){
                        novel.setHiatus(Boolean.parseBoolean(data[12]));
                    }

                    novelList.add(novel);   //메모리 리스트에 로드
                    addNovelCard(novel);    //화면에 카드 출력
                }
            }
            //로드 직후 첫 컴포넌트 뷰 빌드 동기화
            refreshLibrary();
        } catch(IOException e){
            System.out.println("보관함 데이터 로드 실패: " + e.getMessage());
        }
    }

    //상세 정보창에서 호출하여 메모리와 하드디스크에서 소설을 영구 제거하는 메서드
    public void deleteNovel(Novel novel){
        if("패러디".equals(novel.getGenre()) && parodyPanel != null){
            parodyPanel.deleteNovel(novel);  //패러디 서재로 삭제 로직 우회
        } else if(novelList.remove(novel)){
            saveLibraryData();
            refreshLibrary();
        }
    }

    //상세창에서 작가 클릭 시 내 서재 메뉴 상태로 강제 복귀시키는 제어 인터페이스
    public void triggerMyLibraryMenu(){
        if("패러디 서재".equals(this.currentMenu)){
            if(parodyPanel != null){
                parodyPanel.loadParodyMetadata();   //파일에서 최신 기록 동기화
            }
        } else if("단편/썰 서재".equals(this.currentMenu)){
            if(shortStoryPanel != null){
                shortStoryPanel.refreshShortStoryLibrary();
            }
        } else{
            this.currentMenu = "내 서재";
            loadLibraryData();  //파일에서 최신 기록 동기화 후 새로고침
        }

        //원격 호출이므로 안전한 Swing 스레드 동기화를 통해 버튼 배경색 피드백 리셋 처리
        java.awt.Component[] comps = mainFrame.getContentPane().getComponents();
        //기본적으로 refreshLibrary 내에서 색상 관리가 수행되므로 플래그 전환 후 리프레시
    }

    //작가 이름을 전달받아 검색창 텍스트를 강제 수정하고 필터 엔진을 깨우는 원격 조종 매커니즘
    public void searchByAuthor(String authorName){
        if(searchField != null){
            searchField.setText(authorName);
            searchField.setForeground(Color.BLACK);     //회색 해제 및 흑색 전환
            refreshLibrary();   //검색 엔진 즉시 가동
        }
    }

    //로고 이미지 파일을 안전하게 로드하고 스케일링하여 라벨 객체로 반환하는 전용 서포트 엔진
    private JLabel createPlatformLogoLabel(String imagePath, String fallbackText){
        JLabel lblLogo = new JLabel();
        File logoFile = new File(imagePath);

        //지정된 경로에 실제 로고 이미지 파일이 존재할 때만 그래픽 인쇄 가동
        if(logoFile.exists()){
            try{
                ImageIcon originalIcon = new ImageIcon(imagePath);
                //로고 가로 비율 유지를 위해 높이 14 구격으로 모던 리사이징 실행
                int width = (int) ((double) originalIcon.getIconWidth() * (14.0 / originalIcon.getIconHeight()));
                //너비가 너무 비대해져서 정렬을 해치지 않도록 최대 폭을 50으로 제한
                Image scaledImg = originalIcon.getImage().getScaledInstance(width > 50 ? 50 : width, 14, Image.SCALE_SMOOTH);
                lblLogo.setIcon(new ImageIcon(scaledImg));
                lblLogo.setToolTipText(fallbackText);       //마우스 올리면 플랫폼 이름 팝업 힌트 노출
            } catch(Exception e){
                //이미지 로드 실패 시 텍스트로 안전하게 대체
                lblLogo.setText("[" + fallbackText + "]");
                lblLogo.setFont(new Font("맑은 고딕", Font.BOLD, 10));
                lblLogo.setForeground(new Color(255, 120, 0));
            }
        } else{
            //아직 로고 이미지 파일이 배치되지 않은 상태일 때는 가드 텍스트 출력
            lblLogo.setText("[" + fallbackText + "]");
            lblLogo.setFont(new Font("맑은 고딕", Font.BOLD, 10));
            lblLogo.setForeground(new Color(255, 120, 0));
        }
        return lblLogo;
    }

    // 4단계 통계 분석 연동용 패스 개방 포트(전역 통계 분석 시 활용 예정)
    public ArrayList<Novel> getNovelList(){
        return novelList;
    }

    //플랫폼 삭제 요청 시 호출되는 스크롤 선택형 동적 JDialog 팝업 빌더 엔진
    private void openDeletePlatformDialog(){
        JDialog delDialog = new JDialog(mainFrame, "플랫폼 삭제 선택", true);
        delDialog.setSize(340, 420);
        delDialog.setLocationRelativeTo(mainFrame);
        delDialog.setLayout(new BorderLayout(0, 10));
        delDialog.getContentPane().setBackground(Color.WHITE);

        //상단 타이틀 배너
        JLabel lblHeader = new JLabel("삭제할 플랫폼을 목록에서 선택하세요.", SwingConstants.CENTER);
        lblHeader.setFont(UiStyle.FONT_BOLD_13);
        lblHeader.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
        delDialog.add(lblHeader, BorderLayout.NORTH);

        // 중앙 구역: 실시간 수집된 플랫폼 리스트를 Swing 스크롤 모델 배열로 가공 적재
        ArrayList<String> currentPlatforms = AppSettings.getInstance().getCustomPlatforms();
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for(String p : currentPlatforms){
            listModel.addElement(p);
        }

        //스크롤이 지원되는 리스트 컴포넌트
        JList<String> platformJList = new JList<>(listModel);
        platformJList.setFont(UiStyle.FONT_PLAIN_13);

        //다중 선택 모드(여러 플랫폼을 동시에 체크 가능)
        platformJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        platformJList.setCellRenderer(new ListCellRenderer<String>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
                JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
                JLabel lblIcon = new JLabel();
                JLabel lblText = new JLabel(value);

                lblText.setFont(UiStyle.FONT_PLAIN_13);

                if(isSelected){
                    //선택 시 배경색이 어두워지고 기호가 검은 네모로 채워짐
                    rowPanel.setBackground(new Color(230, 235, 245));
                    lblIcon.setText("▣");
                    lblIcon.setForeground(Color.BLACK);
                    lblText.setForeground(Color.BLACK);
                } else{
                    //미선택 시 하얀 배경의 네모 유지
                    rowPanel.setBackground(Color.WHITE);
                    lblIcon.setText("□");
                    lblIcon.setForeground(Color.GRAY);
                    lblText.setForeground(Color.DARK_GRAY);
                }

                rowPanel.add(lblIcon);
                rowPanel.add(lblText);
                rowPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 240, 240)));  //행 경계선
                return rowPanel;
            }
        });

        // 1. Swing 자체의 기본 마우스 선택 이벤트를 완전히 삭제, 초기화 버그 차단
        for(java.awt.event.MouseListener ml : platformJList.getMouseListeners()){
            if(ml.getClass().getName().contains("MouseInputHandler") ||
            ml.getClass().getName().contains("BasicListUI")) {
                platformJList.removeMouseListener(ml);
            }
        }

        //2. 마우스 버튼이 눌렀다가 떨어지는 시점에서 행 전체를 정밀 타격하여 상태를 반전
        platformJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e)){
                    //마우스 좌표를 기반으로 유저가 찌른 행의 인덱스 번호를 정확히 역산
                    int index = platformJList.locationToIndex(e.getPoint());
                    if(index != -1){
                        //해당 행의 바운더리 마진 영역을 계산하여 정위치 클릭 검증
                        Rectangle cellBounds = platformJList.getCellBounds(index, index);
                        if(cellBounds != null && cellBounds.contains(e.getPoint())){
                            //이미 선택된 항목이면 선택 해제, 미선택 항목이면 기존 데이터를 보존한 채 다중 추가(toggle)
                            if(platformJList.isSelectedIndex(index)){
                                platformJList.removeSelectionInterval(index, index);
                            } else{
                                platformJList.addSelectionInterval(index, index);
                            }

                            //그래픽 렌더러단에 Icon 기호 변경 신호를 강제 통보(화면 갱신)
                            platformJList.repaint();
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(platformJList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(215, 220, 225), 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(12); //스크롤 부드럽게 세팅

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(Color.WHITE);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
        centerWrapper.add(scrollPane, BorderLayout.CENTER);
        delDialog.add(centerWrapper, BorderLayout.CENTER);

        //하단 제어 조작 버튼 구역(적색 삭제 버튼 + 취소 버튼)
        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        bottomButtonPanel.setBackground(new Color(248, 249,250));
        bottomButtonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 235, 240)));

        JButton btnSubmitDelete = new JButton("선택 항목 영구 삭제");
        btnSubmitDelete.setFont(UiStyle.FONT_BOLD_12);
        btnSubmitDelete.setBackground(new Color(255, 230, 230));
        btnSubmitDelete.setForeground(Color.RED);
        btnSubmitDelete.setFocusPainted(false);
        btnSubmitDelete.setPreferredSize(new Dimension(140, 30));

        JButton btnClosingDialog = new JButton("취소");
        btnClosingDialog.setFont(UiStyle.FONT_PLAIN_12);
        btnClosingDialog.setBackground(Color.WHITE);
        btnClosingDialog.setFocusPainted(false);
        btnClosingDialog.setPreferredSize(new Dimension(80, 30));

        //삭제 로직 액션 이벤트 바인딩
        btnSubmitDelete.addActionListener(e -> {
            java.util.List<String> selectedValue = platformJList.getSelectedValuesList();
            if(selectedValue == null){
                JOptionPane.showMessageDialog(delDialog, "삭제할 플랫폼이 선택되지 않았습니다.", "경고", JOptionPane.WARNING_MESSAGE);
                return;
            }

            //기본 필수 플랫폼 삭제 시도 가드 처리 행
            for(String val : selectedValue){
                if(val.equals("네이버 시리즈") || val.equals("카카오페이지") ||
                        val.equals("조아라") || val.equals("문피아")){
                    JOptionPane.showMessageDialog(delDialog, "시스템 기본 내장 플랫폼은 내장 무결성 보존을 위해 삭제할 수 없습니다.",
                            "안내", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            int confirm = JOptionPane.showConfirmDialog(delDialog, "[" + selectedValue + "] 플랫폼을 보관함 설정에서 영구 제거하시겠습니까?",
                    "삭제 최종 확인", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            if(confirm == JOptionPane.YES_OPTION){
                //현재 선택된 삭제 대상 플랫폼 목록을 순회
                for(String delTarget : selectedValue){
                    //비교 정밀도를 위해 양끝 공백 전처리 소독
                    String targetClean = delTarget.trim();

                    for(Novel novel : novelList){
                        String currentPlatformStr = novel.getPlatform();
                        if(currentPlatformStr.contains(delTarget)){
                            //1. 해당 플랫폼 명칭을 공백과 쉼표를 고려하여 깨끗하게 제거
                            String cleaned = currentPlatformStr
                                    .replace(delTarget, "")
                                    .replaceAll(",\\s*,", ",")  //중간 쉼표 중복 정돈
                                    .replaceAll("^\\s*,|,\\s*$", "")    //맨 앞이나 맨 뒤 쉼표 제거
                                    .trim();

                            if(cleaned.isEmpty()) cleaned = "기타";

                            //2. 청소 완료된 텍스트 사양을 소석 객체 백엔드에 재주입
                            novel.setPlatforms(cleaned);
                        }
                    }

                    //Appsettings 메모리 안에서 타겟 플랫폼 이름의 방 번호(인덱스)를 역추적하여
                    //이미지 경로 사앚에서도 해당 번호의 칸을 함께 삭제
                    int targetIndex = AppSettings.getInstance().getCustomPlatforms().indexOf(targetClean);
                    if(targetIndex != -1 && targetIndex < AppSettings.getInstance().getCustomPlatformIcons().size()){
                        AppSettings.getInstance().getCustomPlatformIcons().remove(targetIndex);
                    }
                }
                //전수 조사가 종료되었으므로 소설 텍스트 파일(library_data.txt)에 영구 동기화 세이브
                saveLibraryData();

                //메모리 잔상으로 남은 소설 정보들을 완전히 청소하고 최신화된 파일 데이터로 서재 전체를 리부팅
                libraryGridPanel.removeAll();       //기존 UI카드 컴포넌트 완전 제거
                loadLibraryData();                  //최신 세이브본 기반 메모리 로드 및 카드 재인쇄 트리거

                //1. AppSettings 전역 메모리 클래스 내부 어레이리스트에서 대상 항목 제거 후 영구 세이브 연동
                AppSettings.getInstance().getCustomPlatforms().removeAll(selectedValue);
                AppSettings.getInstance().saveSettings();

                //2. 상단 메인 검색 콤보박스 파이프라인 연동 실시간 업데이트 동기화 가동
                java.awt.event.ActionListener tempListener = platformCombo.getActionListeners()[0];
                platformCombo.removeActionListener(tempListener);

                Object currentSelected = platformCombo.getSelectedItem();
                platformCombo.removeAllItems();
                platformCombo.addItem("전체 플랫폼");
                for(String p : AppSettings.getInstance().getCustomPlatforms()){
                    platformCombo.addItem(p);
                }

                //만약 삭제한 플랫폼을 상단 필터에서 선택하고 있었다면 "전체 플랫폼"으로 안전 롤백
                if(selectedValue.equals(currentSelected)){
                    platformCombo.setSelectedItem("전체 플랫폼");
                } else{
                    platformCombo.setSelectedItem(currentSelected);
                }
                platformCombo.addActionListener(tempListener);

                // 3. 다이얼로그 팝업 내부 스크롤 리스트 상태 최신화 새로고침
                delDialog.dispose();
                JOptionPane.showMessageDialog(delDialog, "플랫폼 정보가 정상적으로 파기되었습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        btnClosingDialog.addActionListener(evt -> delDialog.dispose());

        bottomButtonPanel.add(btnSubmitDelete);
        bottomButtonPanel.add(btnClosingDialog);
        delDialog.add(bottomButtonPanel, BorderLayout.SOUTH);

        delDialog.setVisible(true);
    }

    public ParodyPanel getParodyPanel() { return this.parodyPanel; }

    //탭 버튼 생성 및 상태 동기화 헬퍼 메서드
    private JButton createTabButton(String baseTitle){
        JButton btn = new JButton(baseTitle){
            @Override
            protected void paintComponent(Graphics g){
                super.paintComponent(g);

                //현재 선태된 탭을 경우 하단에 청록색 밑줄 드로잉
                if(getText().startsWith(currentStatusTab + " (")){
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(UiStyle.COLOR_ACCENT);
                    g2.fillRect(0, getHeight()-2, getWidth(), 2);
                    g2.dispose();
                }
            }
        };
        btn.setFont(UiStyle.FONT_BOLD_12);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        btn.addActionListener(e -> {
            currentStatusTab = baseTitle;   //필터 상태값 전환
            updateTabStyles();
            refreshLibrary();   //필터 엔진 재가동
        });
        return btn;
    }

    private void selectMenuButton(JButton[] allMenuButtons, JButton selected){
        for(JButton btn : allMenuButtons){
            if(btn == selected){
                btn.setBackground(UiStyle.COLOR_MENU_HIGHLIGHT_BG);
                btn.setForeground(UiStyle.COLOR_ACCENT);
            } else {
                btn.setBackground(Color.WHITE);
                btn.setForeground(UiStyle.COLOR_TEXT_INACTIVE);
            }
        }
    }

    private void updateTabStyles(){
        if(btnTabAll == null) return;
        JButton[] tabs = {btnTabAll, btnTabOngoing, btnTabCompleted, btnTabHiatus};
        for(JButton btn : tabs){
            if(btn.getText().startsWith(currentStatusTab + " (")){
                btn.setForeground(UiStyle.COLOR_ACCENT);
            } else{
                btn.setForeground(UiStyle.COLOR_ICON_INACTIVE);
            }
            btn.repaint();
        }
    }
}