import javax.swing.*;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;

// [단편]/[썰] 전용 텍스트 뷰어
public class ShortStoryViewerPage extends JFrame{
    //뷰어 중복 실행 방지를 위한 전역 인스턴스 추적 변수
    private static ShortStoryViewerPage currentViewerInstance = null;

    //테마 및 가독성 변수
    private int currentFontSize = 16;
    private String currentFontName = "맑은 고딕";
    private Color currentBgColor = Color.WHITE;
    private Color currentFgColor = new Color(33, 33, 33);

    // 데이터 제어 및 컴포넌트 멤버 변수
    private File targetTextFile;
    private java.util.List<File> seriesFiles = new java.util.ArrayList<>();
    private int currentSeriesIndex = -1;

    // 전역 즐겨찾기 파일 고정 경로
    private final String GLOBAL_FAV_FILE = "C:\\novel\\favorite_shorts.txt";

    //전역 즐겨찾기 동기화용 컨텍스트 변수
    private Object parentShelf;
    private Object currentNovelObj;

    public void setNovelContext(Object novel, Object parent) {
        this.currentNovelObj = novel; // Novel이나 Snippet 객체를 범용적으로 저장
        this.parentShelf = parent;
    }

    //레이어 관리 멤버 변수(목록창용)
    private JLayeredPane mainLayeredPane;   //레이어드 팬 추가
    private JPanel dimPanel;    //배경 어둡게 처리용
    private JPanel sideListPanel;

    //상단 메타 레이아웃 분할 컴포넌트 멤버 변수
    private JLabel lblBadge;
    private JLabel lblTitle;
    private JLabel lblSubTitle;
    private JLabel lblAuthor;
    private JLabel lblMetaStats;    // (등록ㆍ글자수ㆍ플랫폼) 통합 레이블
    private JPanel decorLinePanel;

    private String savedAuthor = "작자미상";
    private String savedSubTitle = "";
    private String savedPlatform = "기타";
    private int savedWordCount = 0;

    //본문 및 외곽 틀 컴포넌트
    private JTextArea textArea;
    private JPanel topBarPanel, bottomNavPanel;
    private JLabel statusLabel;
    private JScrollPane scrollPane;
    private JButton btnPrev;
    private JButton btnNext;

    //상단 툴바의 즐겨찾기 버튼(별모양)
    private JButton btnTopFavorite;

    private JButton floatingCloseBtn;   //사이드리스트 패널 바깥에 겹쳐지는 X 버튼

    private JPanel contentPanel;

    private JPanel searchBarPanel;      //검색바 패널
    private JTextField searchField;     //검색어 입력창
    private JLabel searchStatusLabel;   // " 1/5" 같은 개수 표시 라벨
    private java.util.List<Integer> searchResults = new java.util.ArrayList<>();
    private int currentSearchIndex = -1;
    private String currentKeyword = "";

    // 검색어 하이라이트 제어 및 색상 변수
    private java.util.List<Object> highlightTags = new java.util.ArrayList<>();
    private javax.swing.text.DefaultHighlighter.DefaultHighlightPainter normalPainter =
            new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
    private javax.swing.text.DefaultHighlighter.DefaultHighlightPainter currentPainter =
            new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(new Color(255, 150, 0)); // 강조용 주황색

    private void initSearchBar(JPanel parent){
        searchBarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        searchBarPanel.setBackground(new Color(245, 245, 245));
        searchBarPanel.setVisible(false);   //기본은 숨김

        searchField = new JTextField(15);
        searchStatusLabel = new JLabel("0/0");
        searchStatusLabel.setFont(UiStyle.FONT_BOLD_12);

        JButton btnPrevSearch = new JButton("▲")    ;
        JButton btnNextSearch = new JButton("▼");
        JButton btnCloseSearch = new JButton("X");

        JButton[] sBtns = {btnPrevSearch, btnNextSearch, btnCloseSearch};
        for(JButton b : sBtns){
            b.setFont(UiStyle.FONT_BOLD_12);
            b.setFocusPainted(false);
            b.setContentAreaFilled(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }


        // 제어 버튼 리스너 바인딩
        btnNextSearch.addActionListener(e -> navigateSearch(1));
        btnPrevSearch.addActionListener(e -> navigateSearch(-1));

        btnCloseSearch.addActionListener(e -> {
            if(textArea != null){
                textArea.getHighlighter().removeAllHighlights();
            }
            searchBarPanel.setVisible(false);
            searchBarPanel.getParent().revalidate();
            searchBarPanel.getParent().repaint();
        });

        searchField.addActionListener(e -> {
            String keyword = searchField.getText();
            if(keyword.equals(currentKeyword) && !searchResults.isEmpty()){
                navigateSearch(1);  // 아래 화살표와 동일한 다음 탐색 기능 수행
            } else{
                performSearch(keyword); //새로운 검색어일 경우 전체 새로 검색
            }
        });

        //키보드 위/아래 화살표를 통한 검색 결과 탐색 바인딩
        searchField.addKeyListener(new java.awt.event.KeyAdapter(){
            @Override
            public void keyPressed(java.awt.event.KeyEvent e){
                if(e.getKeyCode() == java.awt.event.KeyEvent.VK_UP){
                    navigateSearch(-1);
                } else if(e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN){
                    navigateSearch(1);
                }
            }
        });

        searchBarPanel.add(searchField);
        searchBarPanel.add(searchStatusLabel);
        searchBarPanel.add(btnPrevSearch);
        searchBarPanel.add(btnNextSearch);
        searchBarPanel.add(btnCloseSearch);

        // 헤더 래퍼 패널의 하단에 조립
        parent.add(searchBarPanel, BorderLayout.SOUTH);
    }

    public void setSeriesOrderData(java.util.List<File> files, File currentFile){
        // 넘겨받은 리스트를 정렬 가능한 가변 리스트(ArrayList)로 복제
        this.seriesFiles = (files != null) ? files : new java.util.ArrayList<>();

        // 숫자 기반 자연 정렬 알고리즘 주입
        this.seriesFiles.sort((f1, f2) -> {
            int n1 = extractChapterNumber(f1.getName());
            int n2 = extractChapterNumber(f2.getName());

            if(n1 != n2){
                return Integer.compare(n1, n2); //숫자가 다르면 오름차순 정렬
            }
            return f1.getName().compareTo(f2.getName());    //숫자가 같거나 둘 다 없으면 기본 문자열 정렬
        });

        if(this.targetTextFile == null || !this.seriesFiles.contains(this.targetTextFile)){
            this.targetTextFile = currentFile;
        }

        if(this.seriesFiles.contains(currentFile)){
            this.currentSeriesIndex = this.seriesFiles.indexOf(this.targetTextFile);
        } else{
            this.currentSeriesIndex = -1;
        }
        updateActionButtonsVisibility();
    }
    //파일 이름에서 마지막 숫자를 추출하는 헬퍼 메서드(클래스 내부에 추가)
    private int extractChapterNumber(String name){
        // 한자 上, 中, 下를 감지하여 고정된 정렬 순위 할당
        if (name.contains("上")) return 1;
        if (name.contains("中")) return 2;
        if (name.contains("下")) return 3;

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(name);
        int num = -1;

        //파일명에 포함된 숫자 중 가장 마지막 숫자를 타겟으로 획득(ex. 제목 (12) -> 12)
        while(m.find()){
            num = Integer.parseInt(m.group(1));
        }
        // 번호가 없는 파일(ex. 외전.txt (完).txt 등)은 맨 뒤로 보내기 위해 최대값 부여
        return (num == -1) ? Integer.MAX_VALUE : num;
    }

    private void updateActionButtonsVisibility(){
        if(btnPrev == null || btnNext == null) return;
        if(currentSeriesIndex == -1 || seriesFiles.isEmpty()){
            //일반 단편이나 썰일 때는 내비게이션 비활성화
            btnPrev.setEnabled(false); btnPrev.setText("< 단일화");
            btnNext.setEnabled(false); btnNext.setText("단일화 >");
        } else{
            //연작일 때 작동 락 해제
            btnPrev.setEnabled(currentSeriesIndex > 0);
            btnPrev.setText(currentSeriesIndex > 0 ? "< 이전 화" : "< 첫 화");
            btnNext.setEnabled(currentSeriesIndex < seriesFiles.size() - 1);
            btnNext.setText(currentSeriesIndex < seriesFiles.size() - 1 ? "다음 화 >" : "마지막 화 >");
        }
    }

    public void openViewer(File textFile) {
        // 기존에 열려있는 뷰어 인스턴스가 있다면 파기
        if (currentViewerInstance != null && currentViewerInstance != this) {
            currentViewerInstance.dispose();
        }
        currentViewerInstance = this;

        this.targetTextFile = textFile;

        // 연작일 경우 마지막으로 읽었던 회차 복원(이어보기)
        if(this.targetTextFile != null && this.targetTextFile.getAbsolutePath().contains("연작")){
            File dir = this.targetTextFile.getParentFile();
            File bookmarkFile = new File(dir, "bookmark.txt");

            if (bookmarkFile.exists()) {
                // [핵심 수정] 특수문자(■ 등)가 있을 경우 깨짐 방지를 위해 UTF-8 명시
                try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(bookmarkFile), "UTF-8"))){
                    String lastFileName = br.readLine();
                    if(lastFileName != null && !lastFileName.trim().isEmpty()){
                        lastFileName = lastFileName.trim();
                        File savedFile = new File(dir, lastFileName);

                        // 1. 파일이 실존하는지 확인 후 타겟으로 먼저 확정 (seriesFiles가 비어있어도 무조건 작동)
                        if (savedFile.exists()) {
                            this.targetTextFile = savedFile;

                            // 2. 만약 외부에서 setSeriesOrderData가 먼저 실행되어 리스트가 있다면 인덱스도 동기화
                            if (!seriesFiles.isEmpty()) {
                                for(int i = 0; i < seriesFiles.size(); i++){
                                    if(seriesFiles.get(i).getName().equals(lastFileName)){
                                        this.currentSeriesIndex = i;
                                        break;
                                    }
                                }
                                updateActionButtonsVisibility();
                            }
                        }
                    }
                }catch (Exception e){
                    System.out.println("연작 책갈피 로드 실패: " + e.getMessage());
                }
            }
        }

        // 1. 프레임 윈도우 기본 사양 정의(타이틀바 제거 및 모던 와이드 핏)
        setTitle("모던 단편/썰/연작 뷰어");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);    //화면 정중앙 배치

        //프레임 메인 컨테이너 레이아웃 축 고정
        setLayout(new BorderLayout(0, 0));
        getContentPane().setLayout(new BorderLayout(0, 0));

        //레이어드 팬을 루트 컨테이너로 설정
        mainLayeredPane = new JLayeredPane();
        setContentPane(mainLayeredPane);

        //기본 화면용 패널 생성
        JPanel basePanel = new JPanel(new BorderLayout(0, 0));
        mainLayeredPane.add(basePanel, JLayeredPane.DEFAULT_LAYER);

        // basePanel 크기를 자동 조절하는 동적 리스너
        mainLayeredPane.addComponentListener(new java.awt.event.ComponentAdapter(){
            @Override
            public void componentResized(java.awt.event.ComponentEvent e){
                int w = mainLayeredPane.getWidth();
                int h = mainLayeredPane.getHeight();

                // 메인 바탕 패널을 실면적에 딱 맞게 세팅(하단바가 밖으로 잘라나가지 않음)
                basePanel.setBounds(0, 0, w, h);

                // 창 너비가 변할 때 여백을 다시 계산하여 적용
                int newMargin = Math.max(40, (int)(w * 0.15));
                contentPanel.setBorder(BorderFactory.createEmptyBorder(40, newMargin, 40, newMargin));

                textArea.setSize(new Dimension(w - (newMargin * 2), textArea.getHeight()));

                // 사이드바가 열려있을 때 창 크기를 조절해도 잘리지 않도록 위치 동기화
                if(dimPanel != null) dimPanel.setBounds(0, 0, w, h);
                if(sideListPanel != null) sideListPanel.setBounds(w - 400, 0, 400, h);
                if(floatingCloseBtn != null) floatingCloseBtn.setBounds(w - 444, 24, 32, 32);

                basePanel.revalidate();
                basePanel.repaint();
            }
        });

        //AppSettings에 동기화된 기본 설정을 뷰어 기동 시 최우선 자동 로드 매핑
        this.currentFontName = AppSettings.getInstance().getDefaultFontName();
        this.currentFontSize = AppSettings.getInstance().getDefaultFontSize();

        String savedTheme = AppSettings.getInstance().getDefaultTheme();
        if (savedTheme.equals("베이지색")) {
            this.currentBgColor = new Color(237, 231, 216);
            this.currentFgColor = new Color(60, 52, 44);
        } else if (savedTheme.equals("검정(블랙)")) {
            this.currentBgColor = new Color(30, 37, 41);
            this.currentFgColor = new Color(206, 212, 218);
        } else {
            this.currentBgColor = Color.WHITE;
            this.currentFgColor = new Color(33, 33, 33);
        }

        // 2, 단만 모던 레이아웃 빌드(상단바와 검색바를 하나의 래퍼로 묶음)
        JPanel headerWrapper = new JPanel(new BorderLayout());
        initTopBar(headerWrapper);
        initSearchBar(headerWrapper);
        basePanel.add(headerWrapper, BorderLayout.NORTH);

        initCenterBody(basePanel);
        initBottomNav(basePanel);

        //초기 데이터 세팅 및 테마 인쇄
        loadShortStoryContent();
        changeTheme(currentBgColor, currentFgColor);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveSeriesBookmark();   //창 닫을 때 마지막 회차 파일명 기록
                dispose();
            }
        });

        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("control F"), "showSearch");
        textArea.getActionMap().put("showSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                searchBarPanel.setVisible(true);
                searchField.requestFocusInWindow();
                searchBarPanel.getParent().revalidate();
                searchBarPanel.getParent().repaint();
            }
        });

        //창 활성화 드러내기
        setVisible(true);
    }

    //실제 하이라이트를 적용할 메서드
    private void performSearch(String keyword){
        if(textArea == null) return;

        javax.swing.text.Highlighter h = textArea.getHighlighter();
        h.removeAllHighlights();
        searchResults.clear();
        highlightTags.clear();  //태그 리스트 초기화
        currentSearchIndex = -1;
        currentKeyword = keyword;

        if(keyword.trim().isEmpty()){
            searchStatusLabel.setText("0/0");
            return;
        }

        String content = textArea.getText();
        int index = content.indexOf(keyword);

        while(index >= 0){
            try{
                // 최초 검샏 시에는 모든 일치 단어를 기본색으로 칠하고 태그 보관(노란색)
                Object tag = h.addHighlight(index, index + keyword.length(), normalPainter);
                highlightTags.add(tag);
                searchResults.add(index);

                //다음 단어 찾기
                index = content.indexOf(keyword, index + keyword.length());
            } catch(Exception ex) { break; }
        }

        if(!searchResults.isEmpty()){
            // 첫번째 검색 결과를 강조하기 위해 방향 인덱스 1로 강제 점프 가동
            navigateSearch(1);  //첫번째 결과로 이동
        } else{
            searchStatusLabel.setText("0/0");
            JOptionPane.showMessageDialog(this, "일치하는 단어를 찾을 수 없습니다.");
        }
    }

    // 스크롤 뷰포트 강제 추적 및 타겟 색상 스위칭 엔진
    private void navigateSearch(int direction){
        if(searchResults.isEmpty()) return;

        javax.swing.text.Highlighter h = textArea.getHighlighter();

        // 1. 기존에 선택되어 주황색이던 단어를 다시 기본 노란색으로 복구
        if(currentSearchIndex >= 0 && currentSearchIndex < highlightTags.size()){
            try{
                h.removeHighlight(highlightTags.get(currentSearchIndex));
                int oldPos = searchResults.get(currentSearchIndex);
                Object normalTag = h.addHighlight(oldPos, oldPos + currentKeyword.length(), normalPainter);
                highlightTags.set(currentSearchIndex, normalTag);   //바뀐 태그로 교체
            } catch(Exception e){}
        }

        // 2. 방향키 수치에 따른 배열 인덱스 순환 연산
        if(currentSearchIndex == -1){
            currentSearchIndex = 0; //최초 탐색 진입
        } else{
            currentSearchIndex = (currentSearchIndex + direction + searchResults.size()) % searchResults.size();
        }

        // 3. 새로 타겟이 된 인덱스를 찾아 기존 노란색을 지우고 주황색으로 덧칠
        int pos = searchResults.get(currentSearchIndex);
        try{
            h.removeHighlight(highlightTags.get(currentSearchIndex));
            Object currentTag = h.addHighlight(pos, pos + currentKeyword.length(), currentPainter);
            highlightTags.set(currentSearchIndex, currentTag);

            // 화면 뷰포트를 해당 텍스트 좌표로 물리적 이동
            Rectangle viewRect = textArea.modelToView2D(pos).getBounds();
            if(viewRect != null){
                viewRect.y = Math.max(0, viewRect.y - 50);
                viewRect.height += 100;
                textArea.scrollRectToVisible(viewRect);
            }
            textArea.setCaretPosition(pos);
        } catch (Exception ex){}

        searchStatusLabel.setText((currentSearchIndex + 1) + "/" + searchResults.size());
    }

    //즐겨찾기 파일을 한 번만 읽어서 절대경로 Set으로 변환하는 헬퍼 (목록 렌더링 전용)
    private java.util.Set<String> loadFavoritePathSet(){
        java.util.Set<String> favSet = new java.util.HashSet<>();
        File favFile = new File(GLOBAL_FAV_FILE);
        if(favFile.exists()){
            try(BufferedReader br = new BufferedReader(new FileReader(favFile))){
                String line;
                while((line = br.readLine()) != null){
                    String[] parts = line.split("\\|");
                    if(parts.length > 0) favSet.add(parts[0]);
                }
            } catch(Exception e){}
        }
        return favSet;
    }

    //즐겨찾기(별모양) 렌더링 헬퍼 구역
    private boolean isFavorite(File f){
        if(f == null) return false;
        File favFile = new File(GLOBAL_FAV_FILE);
        if(!favFile.exists()) return false;
        try(BufferedReader br = new BufferedReader(new FileReader(favFile))){
            String line;
            while((line = br.readLine()) != null){
                String[] parts = line.split("\\|");
                //파일의 절대경로가 등록되어 있는지 대조
                if(parts.length > 0 && parts[0].equals(f.getAbsolutePath())){
                    return true;
                }
            }
        } catch(Exception e){}
        return false;
    }

    private void toggleFavorite(File f){
        if(f == null) return;
        boolean currentFav = isFavorite(f);
        File favFile = new File(GLOBAL_FAV_FILE);
        java.util.List<String> favs = new java.util.ArrayList<>();

        // 데이터 형식: 회차번호|파일명
        String currentData = f.getAbsolutePath()+ "|" + extractChapterNumber(f.getName()) + "|" + f.getName();

        if(favFile.exists()){
            try(BufferedReader br = new BufferedReader(new FileReader(favFile))){
                String line;
                while((line = br.readLine()) != null){
                    String[] parts = line.split("\\|");
                    // 현재 파일의 절대경로와 일치하지 않는 것들만 리스트에 보존
                    if(parts.length > 0 && !parts[0].equals(f.getAbsolutePath())){
                        favs.add(line);
                    }
                }
            } catch(Exception e){}
        }

        if(!currentFav) favs.add(currentData);

        try(BufferedWriter bw = new BufferedWriter(new FileWriter(favFile))){
            for(String fav : favs){
                bw.write(fav);
                bw.newLine();
            }
        } catch(Exception e) {}

        //현재 즐겨찾기 상태를 외부 서재(보관함) 패널과 실시간 동기화
        if (currentNovelObj != null) {
            boolean hasAnyFav = !favs.isEmpty();

            // Novel 객체인지 Snippet 객체인지 판별하여 상태 업데이트
            if (currentNovelObj instanceof Novel) {
                ((Novel) currentNovelObj).setFavorite(hasAnyFav);
                ((Novel) currentNovelObj).setFavoriteTimestamp(hasAnyFav ? System.currentTimeMillis() : 0);
            } else if (currentNovelObj instanceof Snippet) {
                ((Snippet) currentNovelObj).setFavorite(hasAnyFav);
            }

            // 서재 화면 새로고침
            if (parentShelf != null) {
                try {
                    parentShelf.getClass().getMethod("saveLibraryData").invoke(parentShelf);
                    try {
                        parentShelf.getClass().getMethod("refreshShortStoryLibrary").invoke(parentShelf);
                    } catch (Exception ex) {
                        parentShelf.getClass().getMethod("refreshLibrary").invoke(parentShelf);
                    }
                } catch (Exception e) {}
            }
        }
    }

    private Icon createStarIcon(boolean isFilled){
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if(isFilled){
                    g2.setColor(new Color(110, 90, 200));   //채워진 보라색 별
                } else{
                    g2.setColor(new Color(180, 185, 190));  //빈 회색 테두리 별
                    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                }

                //별 모양을 그리기 위한 10개의 꼭짓점 정밀 좌표
                int[] px = {x+9, x+11, x+18, x+12, x+15, x+9, x+3, x+6, x+0, x+7};
                int[] py = {y+0, y+6, y+6, y+11, y+18, y+14, y+18, y+11, y+6, y+6};

                if(isFilled) g2.fillPolygon(px, py, 10);
                else g2.drawPolygon(px, py, 10);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 18;
            }

            @Override
            public int getIconHeight() {
                return 18;
            }
        };
    }

    //상단 툴바 조립 구역
    private void initTopBar(JPanel parent) {
        topBarPanel = new JPanel(new BorderLayout());
        topBarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 242, 245)));
        topBarPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 45));

        //왼쪽 경로 표기 레이블
        String originName = (targetTextFile != null) ? targetTextFile.getParentFile().getParentFile().getName() : "원작";
        JLabel lblLeft = new JLabel(" ← " + originName + " 2차  |  2차: " + originName);
        lblLeft.setFont(UiStyle.FONT_PLAIN_12);
        lblLeft.setForeground(Color.GRAY);

        //우측 도구 제어 그룹
        JPanel rightBtnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 8));
        rightBtnGroup.setOpaque(false);

        // 즐겨찾기 버튼
        btnTopFavorite = new JButton();
        btnTopFavorite.setFocusPainted(false);
        btnTopFavorite.setContentAreaFilled(false);
        btnTopFavorite.setBorderPainted(false);
        btnTopFavorite.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTopFavorite.setPreferredSize(new Dimension(30, 30));
        btnTopFavorite.setIcon(createStarIcon(isFavorite(targetTextFile)));
        btnTopFavorite.addActionListener(e -> {
            toggleFavorite(targetTextFile);
            btnTopFavorite.setIcon(createStarIcon(isFavorite(targetTextFile)));
            if(sideListPanel != null && sideListPanel.isShowing()){
                showSideList(); //리스트 창이 열려있다면 새로고침
            }
        });
        rightBtnGroup.add(btnTopFavorite);

        //목록 보기 버튼
        JButton btnList = new JButton(){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(60, 60, 60));
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                //짝대기 3개 그리기
                int w = getWidth();
                int h = getHeight();
                g2.drawLine(w/4, h/3, w*3/4, h/3);
                g2.drawLine(w/4, h/2, w*3/4, h/2);
                g2.drawLine(w/4, h*2/3, w*3/4, h*2/3);
                g2.dispose();
            }
        };
        btnList.setPreferredSize(new Dimension(30, 30));
        btnList.setContentAreaFilled(false);
        btnList.setBorderPainted(false);
        btnList.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnList.addActionListener(e -> showSideList());
        rightBtnGroup.add(btnList);

        //톱니바퀴 환경설정 버튼
        ImageIcon settingImg = new ImageIcon("C:\\novel\\icon\\setting_icon.png");
        JButton btnSettings = new JButton(new ImageIcon(settingImg.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
        btnSettings.setBorderPainted(false);
        btnSettings.setContentAreaFilled(false);
        btnSettings.setFocusPainted(false);
        btnSettings.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSettings.addActionListener(e -> showSettingDialog(this));
        rightBtnGroup.add(btnSettings);

        topBarPanel.add(lblLeft, BorderLayout.WEST);
        topBarPanel.add(rightBtnGroup, BorderLayout.EAST);
        parent.add(topBarPanel, BorderLayout.NORTH);
    }

    //사이드 목록창
    private void showSideList(){
        if(dimPanel != null) mainLayeredPane.remove(dimPanel);
        if(sideListPanel != null) mainLayeredPane.remove(sideListPanel);
        if(floatingCloseBtn != null) mainLayeredPane.remove(floatingCloseBtn);

        // 1. 반투명 오버레이
        dimPanel = new JPanel(){
            @Override
            protected void paintComponent(Graphics g){
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, 150));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        dimPanel.setOpaque(false);
        dimPanel.setBounds(0, 0, mainLayeredPane.getWidth(), mainLayeredPane.getHeight());
        dimPanel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { closeSideList(); }
        });
        mainLayeredPane.add(dimPanel, JLayeredPane.PALETTE_LAYER);

        // 2. 우측 회차 목록 패널
        int panelWidth = 400;
        sideListPanel = new JPanel(new BorderLayout());
        sideListPanel.setBackground(Color.WHITE);
        sideListPanel.setBounds(mainLayeredPane.getWidth() - panelWidth, 0, panelWidth, mainLayeredPane.getHeight());

        // 헤더: 타이틀(좌) + 전체 개수(우) — X버튼은 별도 플로팅으로 분리
        JPanel listHeader = new JPanel(new BorderLayout());
        listHeader.setBackground(Color.WHITE);
        listHeader.setBorder(BorderFactory.createEmptyBorder(24, 20, 15, 20));

        JLabel lblListTitle = new JLabel("회차 목록");
        lblListTitle.setFont(new Font("맑은 고딕", Font.BOLD, 17));
        listHeader.add(lblListTitle, BorderLayout.WEST);

        java.util.List<File> list = new java.util.ArrayList<>();
        java.util.List<File> sourceList = seriesFiles.isEmpty() ? java.util.List.of(targetTextFile) : seriesFiles;
        for(File f : sourceList){
            String name = f.getName().toLowerCase();
            if(name.equals("favorites.txt") || name.equals("bookmark.txt") || name.startsWith("summary") || !name.endsWith(".txt")) continue;
            list.add(f);
        }

        JLabel lblTotalCount = new JLabel("전체 " + list.size() + "개");
        lblTotalCount.setFont(UiStyle.FONT_PLAIN_12);
        lblTotalCount.setForeground(Color.GRAY);
        listHeader.add(lblTotalCount, BorderLayout.EAST);

        sideListPanel.add(listHeader, BorderLayout.NORTH);

        // 리스트 본문
        JPanel listContent = new JPanel();
        listContent.setLayout(new BoxLayout(listContent, BoxLayout.Y_AXIS));
        listContent.setBackground(Color.WHITE);

        java.util.Set<String> favoritePaths = loadFavoritePathSet();   //목록 그리기 전 딱 한 번만 읽어서 캐싱
        for(File f : list){
            listContent.add(createChapterRowPanel(f, favoritePaths));
            listContent.add(Box.createVerticalStrut(0));
        }

        JScrollPane scrollPane = new JScrollPane(listContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // [수정] 실제로 스크롤 값을 갱신하도록 로직 추가 (기존엔 consume()만 하고 있어 스크롤이 안 됐음)
        scrollPane.addMouseWheelListener(e -> {
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            verticalBar.setValue(verticalBar.getValue() + (e.getUnitsToScroll() * verticalBar.getUnitIncrement()));
            e.consume();
        });

        sideListPanel.add(scrollPane, BorderLayout.CENTER);
        mainLayeredPane.add(sideListPanel, JLayeredPane.POPUP_LAYER);

        // 3. [신규] 플로팅 원형 X 버튼 — 패널 좌측 상단 모서리 바깥쪽에 겹쳐서 배치
        floatingCloseBtn = new JButton("X") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) g2.setColor(new Color(255, 90, 90, 210));
                else g2.setColor(new Color(60, 60, 60, 170));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        floatingCloseBtn.setMargin(new Insets(0, 0, 0, 0));
        floatingCloseBtn.setFocusPainted(false);
        floatingCloseBtn.setBorderPainted(false);
        floatingCloseBtn.setContentAreaFilled(false);

        floatingCloseBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        floatingCloseBtn.setForeground(Color.WHITE);
        floatingCloseBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        floatingCloseBtn.setHorizontalAlignment(SwingConstants.CENTER);
        floatingCloseBtn.setVerticalAlignment(SwingConstants.CENTER);

        floatingCloseBtn.setBounds(mainLayeredPane.getWidth() - panelWidth - 44, 24, 32, 32); // 패널 왼쪽 바깥, 상단에서 24px

        floatingCloseBtn.addActionListener(e -> closeSideList());
        mainLayeredPane.add(floatingCloseBtn, JLayeredPane.MODAL_LAYER);

        mainLayeredPane.revalidate();
        mainLayeredPane.repaint();
    }

    private void closeSideList(){
        mainLayeredPane.remove(dimPanel);
        mainLayeredPane.remove(sideListPanel);
        if(floatingCloseBtn != null) mainLayeredPane.remove(floatingCloseBtn);
        repaint();
    }

    // 모던 리스트의 각 행(row) 컴포넌트를 생성하는 독립 엔진
    private JPanel createChapterRowPanel(File f, java.util.Set<String> favoritePaths){
        boolean isCurrent = f.equals(targetTextFile);
        boolean isFav = favoritePaths.contains(f.getAbsolutePath());   //파일 재탐색 없이 즉시 조회isFavorite(File f)

        JPanel row = new JPanel(new BorderLayout(10, 0)){
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                    @Override public void mouseClicked(MouseEvent e) { switchSeriesChapter(f); closeSideList(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if(isCurrent) g2.setColor(new Color(245, 240, 255));
                else if(hovered) g2.setColor(new Color(250, 250, 252));
                else g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // 카드형 박스 대신 하단 구분선으로 표현(원하시는 디자인 반영)
                g2.setColor(new Color(240, 240, 245));
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(280, 52));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        //좌측 번호 배지
        int num = extractChapterNumber(f.getName());
        String numStr;
        if(f.getName().contains("完") || f.getName().contains("완결")) numStr = "完";
        else if(f.getName().contains("외전")) numStr = "외전";
        else numStr = String.format("%02d", num);

        JLabel lblNum = new JLabel(numStr, SwingConstants.CENTER){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if(isCurrent){
                    g2.setColor(new Color(230, 220, 255));
                } else{
                    if(numStr.equals("完")) g2.setColor(new Color(230, 245, 240));
                    else if(numStr.equals("외전")) g2.setColor(new Color(245, 235, 245));
                    else g2.setColor(new Color(240, 240, 255));
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblNum.setPreferredSize(new Dimension(38, 24));
        lblNum.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        if(isCurrent){
            lblNum.setForeground(new Color(90, 70, 200));
        } else{
            if(numStr.equals("完")) lblNum.setForeground(new Color(0, 140, 120));
            else if(numStr.equals("외전")) lblNum.setForeground(new Color(140, 90, 160));
            else lblNum.setForeground(new Color(110, 90, 200));
        }
        lblNum.setOpaque(false);

        JPanel numWrap = new JPanel(new GridBagLayout());
        numWrap.setOpaque(false);
        numWrap.add(lblNum);

        // [핵심 수정] FlowLayout 대신 row의 BorderLayout.CENTER에 직접 배치 → 줄바꿈으로 인한 텍스트 소실 버그 해결
        String title = f.getName().replace(".txt", "");
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("맑은 고딕", isCurrent ? Font.BOLD : Font.PLAIN, 13));
        lblTitle.setForeground(isCurrent ? new Color(110, 90, 200) : new Color(50, 50, 50));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        //우측 즐겨찾기 버튼
        JButton btnRowFav = new JButton();
        btnRowFav.setIcon(createStarIcon(isFav));
        btnRowFav.setPreferredSize(new Dimension(28, 28));
        btnRowFav.setFocusPainted(false);
        btnRowFav.setBorderPainted(false);
        btnRowFav.setContentAreaFilled(false);
        btnRowFav.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRowFav.addActionListener(e -> {
            toggleFavorite(f);
            btnRowFav.setIcon(createStarIcon(isFavorite(f)));
            if(isCurrent && btnTopFavorite != null){
                btnTopFavorite.setIcon(createStarIcon(isFavorite(f)));
            }
        });

        row.add(numWrap, BorderLayout.WEST);
        row.add(lblTitle, BorderLayout.CENTER);
        row.add(btnRowFav, BorderLayout.EAST);

        return row;
    }


    //본문 영역 조립 구역
    private void initCenterBody(JPanel parent){
        // 1. 마스터 패널: 스크롤팬의 전체 영역을 관리
        JPanel masterPanel = new JPanel(new GridBagLayout());
        masterPanel.setBackground(Color.WHITE);

        // 2. 실제 내용물 패널
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 140, 40, 140));

        //1. 원고 분류 배지
        lblBadge = new JLabel("", JLabel.CENTER){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                g2.dispose();
                super.paintComponent(g);
            }
        };


        lblBadge.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        lblBadge.setAlignmentX(Component.LEFT_ALIGNMENT);

        //2. 작품 제목
        lblTitle = new JLabel();
        lblTitle.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 3. 부제목 라인
        lblSubTitle = new JLabel();
        lblSubTitle.setFont(UiStyle.FONT_PLAIN_12);
        lblSubTitle.setForeground(new Color(110, 115, 120));
        lblSubTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        //가로 정보 행 조립 구역(등록ㆍ글자수ㆍ플랫폼)
        JPanel infoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        infoRow.setOpaque(false);
        infoRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 4. 작가 닉네임 레이블
        lblAuthor = new JLabel();
        lblAuthor.setFont(UiStyle.FONT_BOLD_12);
        lblAuthor.setForeground(new Color(40, 40, 40));

        lblMetaStats = new JLabel();
        lblMetaStats.setFont(UiStyle.FONT_PLAIN_13);
        lblMetaStats.setForeground(UiStyle.COLOR_ICON_INACTIVE);

        infoRow.add(lblAuthor);
        infoRow.add(lblMetaStats);

        // 5. 본문 진입 전 환기용 수평 구분 실선 패널
        decorLinePanel = new JPanel();
        decorLinePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        decorLinePanel.setPreferredSize(new Dimension(0, 1));
        decorLinePanel.setBackground(new Color(230, 235, 240));
        decorLinePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 6. 알맹이 텍스트만 전담 마크할 순정 본문 창
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFocusable(true);
        textArea.setHighlighter(new javax.swing.text.DefaultHighlighter());
        textArea.getCaret().setVisible(true);

        textArea.setLineWrap(true); //자동 줄바꿈 활성화(가로 스크롤 방지)
        textArea.setWrapStyleWord(true);    //단어 단위 끊김 방지
        textArea.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        textArea.setAlignmentX(Component.LEFT_ALIGNMENT);


        //마스터 패널에 위에서부터 순차 적재
        contentPanel.add(lblBadge);          contentPanel.add(Box.createVerticalStrut(14));
        contentPanel.add(lblTitle);          contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(lblSubTitle);       contentPanel.add(Box.createVerticalStrut(12));
        contentPanel.add(infoRow);           contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(decorLinePanel);    contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(textArea);

        // 마스터 패널에 contentPanel을 중앙 배치
        masterPanel.add(contentPanel);

        scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);    //가로 바 전면 차단
        scrollPane.getVerticalScrollBar().setUnitIncrement(18); //스크롤 속도

        scrollPane.addMouseWheelListener(evt -> {
            int rotation = evt.getWheelRotation();
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            int maxScroll = verticalBar.getMaximum() - verticalBar.getVisibleAmount();

            if(rotation < 0){
                //1. 마우스 휠을 위로 올렸을 때(하단바 출현)
                if(!bottomNavPanel.isVisible()){
                    bottomNavPanel.setVisible(true);
                    //레이어드 팬 환경에 맞게 부모 패널을 명시적으로 강제 새로고침
                    bottomNavPanel.getParent().revalidate();
                    bottomNavPanel.getParent().repaint();
                }
            } else if(rotation > 0){
                // 2. 마우스 휠을 아래로 내렸을 때(하단바 은닉)
                // 단, 현재 스크롤이 바닥에 도달하지 않은 상태에서만 숨김 처리 실행
                if(verticalBar.getValue() < maxScroll - 5){
                    if(bottomNavPanel.isVisible()){
                        bottomNavPanel.setVisible(false);
                        bottomNavPanel.getParent().revalidate();
                        bottomNavPanel.getParent().repaint();
                    }
                }
            }
        });

        //사용자가 본문을 읽을 때 실시간으로 게이지 바 눈금을 전진시키는 백엔드 수식 엔진
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            int maxScroll = verticalBar.getMaximum() - verticalBar.getVisibleAmount();
            if(maxScroll <= 0) return;

            //현재 스크롤 백분율 역산
            int progressPercent = (int) ((verticalBar.getValue() / (double) maxScroll) * 100);

            //실시간 컴포넌트 텍스트 및 슬라이더 눈금 락 동기화 주입
            if(statusLabel != null) statusLabel.setText(progressPercent + "%");

            JSlider slider = (JSlider) bottomNavPanel.getClientProperty("sliderRef");
            if(slider != null && !slider.getValueIsAdjusting()){
                slider.setValue(progressPercent);
            }

            // 스크롤이 맨 아래(바닥)에 도달하면 하단 패널 자동 표시
            // UI 렌더링 오차를 감안하여 최대 스크롤 값에서 5픽셀 이내로 접근하면 바닥으로 간주
            if(verticalBar.getValue() >= maxScroll - 5){
                if(!bottomNavPanel.isVisible()){
                    bottomNavPanel.setVisible(true);
                    bottomNavPanel.getParent().revalidate();
                    bottomNavPanel.getParent().repaint();
                }
            }
        });

        parent.add(scrollPane, BorderLayout.CENTER);
    }

    //하단 코글 페이징 바 조립 구역
    private void initBottomNav(JPanel parent){
        bottomNavPanel = new JPanel(new BorderLayout(15, 0));
        bottomNavPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 50));
        bottomNavPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(235, 238, 242)));

        // 1. 좌측 [이전 화] 네비게이션 단추
        btnPrev = new JButton("< 이전 화");
        btnPrev.setFont(UiStyle.FONT_BOLD_12);
        btnPrev.setForeground(Color.GRAY);
        btnPrev.setContentAreaFilled(false);
        btnPrev.setBorderPainted(false);
        btnPrev.setFocusPainted(false);
        btnPrev.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnPrev.addActionListener(e -> {
            if(currentSeriesIndex > 0){
                currentSeriesIndex--;
                switchSeriesChapter(seriesFiles.get(currentSeriesIndex));
            }
        });

        // 2. 우측 [다음 화] 네비게이션 단추
        btnNext = new JButton("다음 화 >");
        btnNext.setFont(UiStyle.FONT_BOLD_12);
        btnNext.setForeground(Color.GRAY);
        btnNext.setContentAreaFilled(false);
        btnNext.setBorderPainted(false);
        btnNext.setFocusPainted(false);
        btnNext.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnNext.addActionListener(e -> {
            if(currentSeriesIndex < seriesFiles.size() - 1){
                currentSeriesIndex++;
                switchSeriesChapter(seriesFiles.get(currentSeriesIndex));
            }
        });

        // 3. 중앙 실시간 진행률 게이지 바
        JSlider progressSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        progressSlider.setOpaque(false);
        progressSlider.setFocusable(false);
        progressSlider.setCursor(new Cursor(Cursor.HAND_CURSOR));

        progressSlider.setMinimumSize(new Dimension(100, 30));
        progressSlider.setPreferredSize(new Dimension(450, 30));
        progressSlider.setMaximumSize(new Dimension(450, 30));

        Color themeCyan = UiStyle.COLOR_ACCENT;
        Color trackGray = new Color(225, 228, 230);

        progressSlider.setUI(new javax.swing.plaf.basic.BasicSliderUI(progressSlider){
            @Override
            public void paintTrack(Graphics g){
                Graphics2D g2= (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                //트랙 중심선 좌표 연산
                int cy = trackRect.y + (trackRect.height / 2) - 2;
                int w = trackRect.width;

                g2.setColor(trackGray);
                g2.fillRoundRect(trackRect.x, cy, w, 4, 2, 2);

                int thumbPos = thumbRect.x + (thumbRect.width / 2);
                int filledWidth = thumbPos - trackRect.x;
                if(filledWidth > 0){
                    g2.setColor(themeCyan);
                    g2.fillRoundRect(trackRect.x, cy, filledWidth, 4, 2, 2);
                }
                g2.dispose();
            }
            @Override
            public void paintThumb(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(themeCyan);
                int tx = thumbRect.x + (thumbRect.width - 12)/2;
                int ty = thumbRect.y + (thumbRect.height-12)/2;
                g2.fillOval(tx, ty, 12, 12);

                g2.dispose();
            }
            @Override
            protected Dimension getThumbSize(){
                return new Dimension(14, 14);
            }
        });

        // 슬라이더 조작 시 몬분 스크롤을 즉시 해당 위치로 점프시키는 액션 바인딩
        progressSlider.addChangeListener(e -> {
            if(progressSlider.getValueIsAdjusting() && scrollPane != null){
                int value = progressSlider.getValue();
                JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
                int maxScroll = verticalBar.getMaximum() - verticalBar.getVisibleAmount();
                int targetScrollPosition = (int) (maxScroll * (value / 100.0));
                verticalBar.setValue(targetScrollPosition);
            }
        });

        // 4. 슬라이더 우측에 퍼센트 수치를 표기해 줄 미니 레이블
        statusLabel = new JLabel("0%", JLabel.CENTER);
        statusLabel.setFont(UiStyle.FONT_BOLD_12);
        statusLabel.setPreferredSize(new Dimension(45, 30));

        // 배치 균형을 유지하기 위한 그룹 배치 패널 조립
        JPanel centerProgressGroup = new JPanel(new BorderLayout(0, 0));
        centerProgressGroup.setOpaque(false);

        centerProgressGroup.setPreferredSize(new Dimension(450, 30));
        centerProgressGroup.setMaximumSize(new Dimension(450, 30));

        centerProgressGroup.add(progressSlider, BorderLayout.CENTER);
        centerProgressGroup.add(statusLabel, BorderLayout.EAST);

        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        centerWrapper.setOpaque(false);
        centerWrapper.add(centerProgressGroup);

        bottomNavPanel.add(btnPrev, BorderLayout.WEST);
        bottomNavPanel.add(centerWrapper, BorderLayout.CENTER);
        bottomNavPanel.add(btnNext, BorderLayout.EAST);

        bottomNavPanel.putClientProperty("sliderRef", progressSlider);

        parent.add(bottomNavPanel, BorderLayout.SOUTH);
    }

    private void loadShortStoryContent() {
        if (targetTextFile == null || !targetTextFile.exists()) {
            textArea.setText("단편 파일을 찾을 수 없습니다.");
            return;
        }

        // 파일 정보 및 타이틀 텍스트 가공
        String titleText = targetTextFile.getName().replace(".txt", "");
        String typeText = "단편";

        //디렉토리 경로 문자열 매칭 기반 타입 분기 역산 추출
        String parentPath = targetTextFile.getAbsolutePath();
        if (parentPath.contains("\\썰\\")) {
            typeText = "썰";
        } else if (parentPath.contains("\\연작\\")) {
            typeText = "연작";
        }

        //상단 메타 자바 순정 컴포넌트 텍스트 실시간 사양 매핑
        lblBadge.setText(" " + typeText + " ");

        if(typeText.equals("단편")) {
            lblBadge.setBackground(new Color(235, 247, 245));
            lblBadge.setForeground(UiStyle.COLOR_ACCENT);
        } else if(typeText.equals("썰")) {
            lblBadge.setBackground(new Color(225, 240, 225));
            lblBadge.setForeground(new Color(235, 110, 20));
        } else {
            lblBadge.setBackground(new Color(242, 240, 255));
            lblBadge.setForeground(new Color(110, 90, 200));
        }

        //배지 둥근 사각형 폭 최소 및 최대 고정 규격 처리
        lblBadge.setPreferredSize(new Dimension(48, 22));
        lblBadge.setMaximumSize(new Dimension(48, 22));

        lblTitle.setText(titleText);

        //  보관해둔 데이터를 다시 꺼내서 사용
        lblSubTitle.setText(this.savedSubTitle.trim().isEmpty() ? " " : this.savedSubTitle);
        lblAuthor.setText(this.savedAuthor);

        //복문 텍스트 파일은 순수 텍스트 줄바꿈만 스트림 가공하여 삽입
        StringBuilder bodySb =  new StringBuilder();
        int totalBytes = (int) targetTextFile.length();
        int calculatedWordCount = (int) (totalBytes/2.5);    //바이트 기반 글자수 측정

        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(targetTextFile), "UTF-8"))){
            String line;
            while((line = br.readLine()) != null){
                bodySb.append(line).append("\n");
            }
            textArea.setText(bodySb.toString());

            //글자수
            String formattedWordCount = String.format("%,d", calculatedWordCount);
            lblMetaStats.setText("ㆍ" + formattedWordCount + "자ㆍ포스타입");

            if(statusLabel != null){
                statusLabel.setText("0%");
            }
            bottomNavPanel.setVisible(false);

            SwingUtilities.invokeLater(() -> {
                if(scrollPane != null){
                    scrollPane.getVerticalScrollBar().setValue(0);
                }
            });
        } catch (Exception e) {
            textArea.setText("원고를 불러오는 중 오류가 발생했습니다.");
        }
    }

    public void setMetaInformation(String author, String subTitle, String platform, int wordCount){
        //들어온 데이터를 전역 변수에 안전하게 보관
        this.savedAuthor = author;
        this.savedSubTitle = subTitle;
        this.savedPlatform = platform;
        this.savedWordCount = wordCount;

        if(lblAuthor != null) lblAuthor.setText(author);
        if(lblSubTitle != null){
            // 소제목이 아예 비어있으면 알 수 없는 숫자 대신 빈칸으로 깔끔하게 처리
            lblSubTitle.setText(subTitle.isEmpty() ? " " : subTitle);
        }
        if(lblMetaStats != null){
            String formattedWordCount = String.format("%,d", wordCount);
            lblMetaStats.setText("ㆍ" + formattedWordCount + "자ㆍ" + platform);
        }
    }

    // 설정을 제어하는 모달 다이얼로그 팝업 엔진
    private void showSettingDialog(JFrame parent) {
        JDialog dialog = new JDialog(parent, "뷰어 설정", true);
        dialog.setSize(440, 350);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());

        //취소 기능을 위해 다이얼로그 오픈 시점의 초기 설정 스냅샷을 백업
        final int backupSize = currentFontSize;
        final String backupFont = currentFontName;
        final Color backupBg = currentBgColor;
        final Color backupFg = currentFgColor;

        Color themeCyan = UiStyle.COLOR_ACCENT;
        Color borderGray = new Color(215, 222, 228);
        Color lineCyan = new Color(200, 225, 225);  //연한 청록색 점선용 컬러

        //메인 콘텐츠 컨테이너 패널 세팅
        JPanel mainContentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                //연한 청록색 점선 스타일
                g2.setColor(lineCyan);
                float[] dash = {4.0f, 4.0f};
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dash, 0.0f));

                //각 행 사이에 점선 배치
                g2.drawLine(25, 62, getWidth() - 25, 62);
                g2.drawLine(25, 134, getWidth() - 25, 134);
                g2.dispose();
            }
        };
        mainContentPanel.setLayout(new BoxLayout(mainContentPanel, BoxLayout.Y_AXIS));
        mainContentPanel.setBackground(Color.WHITE);
        mainContentPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 15, 25));

        // 행1: 글자 크기 조정 구역
        JPanel sizeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        sizeRow.setBackground(Color.WHITE);
        sizeRow.setMaximumSize(new Dimension(400, 45));

        JLabel lblSizeTitle = new JLabel("글자 크기");
        lblSizeTitle.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblSizeTitle.setForeground(UiStyle.COLOR_LABEL_TEXT);
        lblSizeTitle.setPreferredSize(new Dimension(140, 40));
        sizeRow.add(lblSizeTitle);

        //마이너스 조절 버튼 생성(둥근 모서리 디자인)
        JButton btnMinus = new JButton();
        btnMinus.setPreferredSize(new Dimension(36, 32));
        btnMinus.setFocusPainted(false);
        btnMinus.setBorderPainted(false);
        btnMinus.setContentAreaFilled(false);
        btnMinus.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMinus.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(borderGray);
                g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 6, 6);

                g2.setColor(themeCyan);
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(13, 16, 23, 16);    //마이너스 기호 수동 정밀 조준 드로잉
                g2.dispose();
            }
        });

        //중앙 숫자 입력 텍스트 필드 생성
        JTextField tfSizeInput = new JTextField(String.valueOf(currentFontSize), 3);
        tfSizeInput.setHorizontalAlignment(JTextField.CENTER);
        tfSizeInput.setFont(UiStyle.FONT_PLAIN_13);
        tfSizeInput.setForeground(themeCyan);
        tfSizeInput.setPreferredSize(new Dimension(55, 32));
        tfSizeInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderGray, 1, true),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));

        //플러스 조절 버튼 생성(둥근 모서리 디자인)
        JButton btnPlus = new JButton();
        btnPlus.setPreferredSize(new Dimension(36, 32));
        btnPlus.setFocusPainted(false);
        btnPlus.setBorderPainted(false);
        btnPlus.setContentAreaFilled(false);
        btnPlus.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPlus.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(borderGray);
                g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 6, 6);

                g2.setColor(themeCyan);
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(13, 16, 23, 16);    //가로선
                g2.drawLine(18, 11, 18, 21);    //세로선
                g2.dispose();
            }
        });

        JPanel sizeBtnGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        sizeBtnGroup.setBackground(Color.WHITE);
        sizeBtnGroup.add(btnMinus);
        sizeBtnGroup.add(tfSizeInput);
        sizeBtnGroup.add(btnPlus);
        sizeRow.add(sizeBtnGroup);

        // 행2: 글꼴 변경 구역
        JPanel fontRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fontRow.setBackground(Color.WHITE);
        fontRow.setMaximumSize(new Dimension(400, 55));

        JLabel lblFontTitle = new JLabel("글꼴 선택");
        lblFontTitle.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblFontTitle.setForeground(UiStyle.COLOR_LABEL_TEXT);
        lblFontTitle.setPreferredSize(new Dimension(140, 45));
        fontRow.add(lblFontTitle);

        String[] fonts = {"맑은 고딕", "나눔고딕", "바탕체", "돋움", "굴림"};
        JComboBox<String> fontComboBox = new JComboBox<>(fonts);
        fontComboBox.setSelectedItem(currentFontName);  //현재 지정 중인 폰트명이 자동 포커싱되도록 유도
        fontComboBox.setFont(UiStyle.FONT_PLAIN_13);
        fontComboBox.setPreferredSize(new Dimension(198, 32));

        fontComboBox.setBackground(Color.WHITE);
        fontComboBox.setOpaque(false);
        //투박한 각진 기본 외곽선 필터를 제거, 은은한 라운드 선으로 바꿈
        fontComboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderGray, 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 2)
        ));
        fontComboBox.setUI(new javax.swing.plaf.basic.BasicComboBoxUI(){
            @Override
            protected JButton createArrowButton(){
                JButton btn = super.createArrowButton();
                btn.setContentAreaFilled(false);
                btn.setBorder(BorderFactory.createEmptyBorder());
                return btn;
            }
        });
        fontRow.add(fontComboBox);

        // 행3: 테마 변경 구역
        JPanel themeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        themeRow.setBackground(Color.WHITE);
        themeRow.setMaximumSize(new Dimension(400, 55));

        JLabel lblThemeTitle = new JLabel("색상 테마");
        lblThemeTitle.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblThemeTitle.setForeground(UiStyle.COLOR_LABEL_TEXT);
        lblThemeTitle.setPreferredSize(new Dimension(140, 45));
        themeRow.add(lblThemeTitle);

        JButton btnThemeWhite = new JButton("흰색");
        JButton btnThemeSepia = new JButton("베이지");
        JButton btnThemeDark = new JButton("검정");

        JButton[] themeButtons = {btnThemeWhite, btnThemeSepia, btnThemeDark};
        for(JButton btn : themeButtons){
            btn.setPreferredSize(new Dimension(70, 32));
            btn.setFont(UiStyle.FONT_BOLD_12);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        //각 테마 버튼에 고유한 배경/글자색 주입, 선택시 청록색 테두리
        java.util.function.Consumer<String> refreshThemeButtonStyles = (activeTheme) -> {
            // 1. 선택된 활성화 상태에 따른 글자(Foreground) 색상 동적 매핑
            btnThemeWhite.setForeground(activeTheme.equals("흰색") ? new Color(33, 33, 33) : UiStyle.COLOR_ICON_INACTIVE);
            btnThemeSepia.setForeground(activeTheme.equals("베이지") ? new Color(60, 52, 44) : new Color(120, 125, 135));
            btnThemeDark.setForeground(activeTheme.equals("검정") ? Color.WHITE : UiStyle.COLOR_ICON_INACTIVE);

            btnThemeWhite.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
                @Override public void paint(Graphics g, JComponent c) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 6, 6);

                    //활성화 시 청록색 테두리로 강조
                    g2.setColor(activeTheme.equals("흰색") ? themeCyan : borderGray);
                    g2.setStroke(new BasicStroke(activeTheme.equals("흰색") ? 1.5f : 1.0f));
                    g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 6, 6);

                    g2.dispose();
                    super.paint(g, c);
                }
            });
            btnThemeSepia.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
                @Override public void paint(Graphics g, JComponent c) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g2.setColor(new Color(237, 231, 216));
                    g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 6, 6);

                    g2.setColor(activeTheme.equals("베이지") ? themeCyan : borderGray);
                    g2.setStroke(new BasicStroke(activeTheme.equals("베이지") ? 1.5f : 1.0f));
                    g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 6, 6);

                    g2.dispose();
                    super.paint(g, c);
                }
            });
            btnThemeDark.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
                @Override public void paint(Graphics g, JComponent c) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g2.setColor(new Color(30, 37, 41));
                    g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 6, 6);

                    if(activeTheme.equals("검정")){
                        g2.setColor(themeCyan);
                        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    } else{
                        g2.setColor(borderGray);
                        g2.setStroke(new BasicStroke(1.0f));
                    }
                    g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 6, 6);

                    g2.dispose();

                    g.setColor(activeTheme.equals("검정") ? Color.WHITE : UiStyle.COLOR_ICON_INACTIVE);
                    super.paint(g, c);
                }
            });
            dialog.repaint();
        };

        //초기 열렸을 때 선택된 테마의 컬러 활성화 버튼 체크
        String initTheme = AppSettings.getInstance().getDefaultTheme();
        refreshThemeButtonStyles.accept(initTheme.equals("베이지") ? "베이지" : (initTheme.equals("검정(블랙)") ? "검정" : "흰색"));

        JPanel themeBtnGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        themeBtnGroup.setBackground(Color.WHITE);
        themeBtnGroup.add(btnThemeWhite);
        themeBtnGroup.add(btnThemeSepia);
        themeBtnGroup.add(btnThemeDark);
        themeRow.add(themeBtnGroup);

        //분리 절취선 기호 세팅
        mainContentPanel.add(sizeRow);
        mainContentPanel.add(Box.createVerticalStrut(28));  //점선 간격 공간 조율 확보
        mainContentPanel.add(fontRow);
        mainContentPanel.add(Box.createVerticalStrut(28));
        mainContentPanel.add(themeRow);

        // 하단 툴바 구역 액션 단추 (취소/확인 버튼)
        JPanel bottomControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        bottomControlPanel.setBackground(new Color(245, 247, 249));
        bottomControlPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 235, 240)));

        JButton btnCancel = new JButton("취소");
        JButton btnConfirm = new JButton("확인");

        JButton[] bottomBtns = {btnCancel, btnConfirm};
        for(JButton btn : bottomBtns){
            btn.setPreferredSize(new Dimension(80, 32));
            btn.setFont(UiStyle.FONT_BOLD_13);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        btnCancel.setForeground(themeCyan);
        btnCancel.setUI(new javax.swing.plaf.basic.BasicButtonUI(){
            @Override
            public void paint(Graphics g, JComponent c){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 6, 6);

                g2.setColor(themeCyan);
                g2.drawRoundRect(0, 0, c.getWidth()-1, c.getHeight()-1, 6, 6);

                g2.dispose();
                super.paint(g, c);
            }
        });

        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setUI(new javax.swing.plaf.basic.BasicButtonUI(){
            @Override
            public void paint(Graphics g, JComponent c){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(themeCyan);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 6, 6);

                g2.dispose();
                super.paint(g, c);
            }
        });

        bottomControlPanel.add(btnCancel);
        bottomControlPanel.add(btnConfirm);

        //[마이너스 버튼 클랙 액션 리스너]
        btnMinus.addActionListener(e -> {
            if(currentFontSize >= 10){
                currentFontSize -= 2;
                tfSizeInput.setText(String.valueOf(currentFontSize));
                updateFont();
            }
        });

        //[플러스 버튼 클릭 액션 리스너]
        btnPlus.addActionListener(e -> {
            if(currentFontSize <= 40){
                currentFontSize += 2;
                tfSizeInput.setText(String.valueOf(currentFontSize));
                updateFont();
            }
        });

        //[직접 타이핑 입력 시 실시간 데이터 검증 감지기]
        tfSizeInput.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            private void updateSize(){
                SwingUtilities.invokeLater(() -> {
                    try{
                        String text = tfSizeInput.getText().trim();
                        if(!text.isEmpty()){
                            int size = Integer.parseInt(text);
                            //실시간 경계선 검증(10~40 범위 내일 때만 전역 변수 승인)
                            if(size >= 10 && size <= 40){
                                currentFontSize = size;
                                updateFont();
                            }
                        }
                    } catch(NumberFormatException ex){
                        //유저가 숫자가 아닌 영문/오타 타이핑 시 변수를 바꾸지 않고 리턴 포트 유지
                    }
                });
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSize(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSize(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSize(); }
        });

        //설정 창 내부 조작 리스너 액션 매핑
        fontComboBox.addActionListener(e -> {
            currentFontName = (String) fontComboBox.getSelectedItem();
            updateFont();
        });

        btnThemeWhite.addActionListener(e -> {
            currentBgColor = Color.WHITE;
            currentFgColor = new Color(33, 33, 33);
            refreshThemeButtonStyles.accept("흰색");
            changeTheme(currentBgColor, currentFgColor);
        });
        btnThemeSepia.addActionListener(e -> {
            currentBgColor = new Color(237, 231, 216);
            currentFgColor = new Color(60, 52, 44);
            refreshThemeButtonStyles.accept("베이지");
            changeTheme(currentBgColor, currentFgColor);
        });
        btnThemeDark.addActionListener(e -> {
            currentBgColor = new Color(30, 37, 41);
            currentFgColor = new Color(206, 212, 218);
            refreshThemeButtonStyles.accept("검정");
            changeTheme(currentBgColor, currentFgColor);
        });

        // [취소 버튼 동작]: 다이어로그 오픈 때의 스냅샷 백업본으로 롤백 후 종료
        btnCancel.addActionListener(e -> {
            currentFontSize = backupSize;
            currentFontName = backupFont;
            currentBgColor = backupBg;
            currentFgColor = backupFg;
            updateFont();
            changeTheme(currentBgColor, currentFgColor);
            dialog.dispose();
        });

        //[확인 버튼 동작]: 현재 임시 표시 중인 뷰어 상태를 최종 하드웨어 디스크에 영구 고정 저장
        btnConfirm.addActionListener(e -> {
            String themeStr = "흰색";
            if(currentBgColor.getGreen() == 231) themeStr = "베이지색";
            else if(currentBgColor.getRed() == 30) themeStr = "검정(블랙)";

            AppSettings.getInstance().setDefaultFontName(currentFontName);
            AppSettings.getInstance().setDefaultFontSize(currentFontSize);
            AppSettings.getInstance().setDefaultTheme(themeStr);
            AppSettings.getInstance().saveSettings();   //영구 저장소 세이브 트리거 발사
            dialog.dispose();
        });

        dialog.add(mainContentPanel, BorderLayout.CENTER);
        dialog.add(bottomControlPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    //글자 크기가 바뀔때마다 실행되어 화면의 글꼴을 새로 고침 해주는 메서드
    private void updateFont(){
        if(textArea == null) return;

        // 1. 현재 지정된 가독성 폰트와 실시간 연동 크기를 TextArea에 직접 주입
        textArea.setFont(new Font(currentFontName, Font.PLAIN, currentFontSize));

        // 2. 메타 정보 컴포넌트들도 사용자가 설정한 기본 폰트명 축에 맞춰 유연하게 렌더링
        lblTitle.setFont(new Font(currentFontName, Font.BOLD, 20));

        // 3. 스크롤 갱신 리프레시 동기화
        revalidate();
        repaint();
    }

    //호출 시 배경색과 글자색을 받아 모든 컴포넌트의 테마를 일괄 변경하는 메서드
    private void changeTheme(Color backgroundColor, Color foregroundColor){
        if(textArea == null) return;

        // 본문 및 마스터 정렬 컨테이너 배경 동기화
        textArea.setBackground(backgroundColor);
        textArea.setForeground(foregroundColor);

        // 현재 글꼴 상태 정밀 가인
        textArea.setFont(new Font(currentFontName, Font.PLAIN, currentFontSize));

        // 메타 헤더 텍스트 컬러 연동
        lblTitle.setForeground(foregroundColor);
        lblSubTitle.setForeground(foregroundColor.equals(Color.WHITE) ? new Color(180, 185, 190) : new Color(110, 115, 120));
        lblAuthor.setForeground(foregroundColor);

        lblMetaStats.setForeground(foregroundColor.equals(Color.WHITE) ? new Color(150, 155, 165) : new Color(170, 175, 180));

        topBarPanel.setBackground(backgroundColor);
        bottomNavPanel.setBackground(backgroundColor);
        statusLabel.setForeground(foregroundColor);
        scrollPane.getViewport().setBackground(backgroundColor);

        //마스터 내부 정렬 패널 배경색 일괄 정비
        if(scrollPane.getViewport().getView() != null){
            scrollPane.getViewport().getView().setBackground(backgroundColor);
        }
    }

    private void switchSeriesChapter(File nextFile){
        this.targetTextFile = nextFile;

        // 목록을 통한 이동 시 현재 회차 인덱스 강제 동기화
        if(seriesFiles != null && !seriesFiles.isEmpty()){
            this.currentSeriesIndex = seriesFiles.indexOf(nextFile);
        }

        //상단 파일 경로 표기 바 타이틀 최신화
        if(topBarPanel != null && topBarPanel.getComponentCount() > 0){
            String originName = targetTextFile.getParentFile().getParentFile().getParentFile().getName();
            JLabel lblLeft = (JLabel) topBarPanel.getComponent(0);
            lblLeft.setText(" < " + originName + " 2차 | 2차: " + originName);
        }

        //회차가 넘어가면 해당 회차의 즐겨찾기 상탤를 확인해 상단 별 아이콘도 즉시 동기화 변경
        if(btnTopFavorite != null){
            btnTopFavorite.setIcon(createStarIcon(isFavorite(targetTextFile)));
        }

        //데이터 리로드 가동
        loadShortStoryContent();
        updateActionButtonsVisibility();

        //스크롤바 위치 맨 위로 리셋
        if(scrollPane != null){
            scrollPane.getVerticalScrollBar().setValue(0);
        }
    }

    // 연작일 경우 마지막으로 읽은 회차(파일명)을 저장하는 메서드
    private void saveSeriesBookmark(){
        if(targetTextFile != null && targetTextFile.getAbsolutePath().contains("연작")) {
            File dir = targetTextFile.getParentFile();
            File bookmarkFile = new File(dir, "bookmark.txt");
            // [핵심 수정] 특수문자(■ 등) 보존을 위해 UTF-8 명시
            try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(bookmarkFile), "UTF-8"))){
                bw.write(targetTextFile.getName());
            } catch(Exception e){
                System.out.println("연작 책갈피 저장 실패: " + e.getMessage());
            }
        }
    }
}