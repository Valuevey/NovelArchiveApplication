import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

public class ParodyPanel extends JPanel {
    private JFrame mainFrame;
    private BookShelfPage masterShelf;

    // 데이터 인프라 및 검색 필터링용 컴포넌트 변수
    private ArrayList<Novel> parodyList = new ArrayList<>(); // 내 서재와 동일하게 Novel 객체로 통일
    private final String PARODY_DATA_FILE = "C:\\novel\\novels\\parodies\\parody_shorts_data.txt";

    private JPanel libraryGridPanel;
    private JComboBox<String> platformCombo;
    private JComboBox<String> sortCombo;
    private JTextField searchField;
    private JLabel lblTotalCounter;

    private int cardCount = 0;

    //상태 탭바 제어용 변수 신설
    private String currentStatusTab = "전체";
    private JButton btnTabAll, btnTabOngoing, btnTabCompleted, btnTabHiatus;

    public ParodyPanel(JFrame mainFrame, BookShelfPage masterShelf) {
        this.mainFrame = mainFrame;
        this.masterShelf = masterShelf;

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        initToolbar();
        initListBody();

        // 기동과 동시에 패러디 세이브본 스캔 복원
        loadParodyMetadata();


    }

    // 1. 패러디 전용 상단 툴바 구성
    private void initToolbar() {
        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setBackground(Color.WHITE);
        toolbarPanel.setLayout(new BorderLayout());
        toolbarPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));

        // 좌측에 배치할 대제목 + 카운터 배지 결합 바구니
        JPanel leftTitleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftTitleGroup.setOpaque(false);

        JLabel lblPageTitle = new JLabel("패러디 서재");
        lblPageTitle.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        lblPageTitle.setForeground(new Color(25, 30, 40));

        lblTotalCounter = new JLabel("전체 작품: 0개") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(240, 240, 255)); // 보라색 감성 배지 유지
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblTotalCounter.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        lblTotalCounter.setForeground(new Color(110, 90, 200));
        lblTotalCounter.setOpaque(false);
        lblTotalCounter.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        leftTitleGroup.add(lblPageTitle);
        leftTitleGroup.add(lblTotalCounter);

        // 우측 조작 컴포넌트 바구니
        JPanel rightButtonGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightButtonGroup.setOpaque(false);

        // 플랫폼 필터 콤보박스
        ArrayList<String> loadPlatforms = AppSettings.getInstance().getCustomPlatforms();
        ArrayList<String> comboItem = new ArrayList<>();
        comboItem.add("전체 플랫폼");
        comboItem.addAll(loadPlatforms);

        platformCombo = new JComboBox<>(comboItem.toArray(new String[0])) {
            @Override
            protected void paintComponent(Graphics g) {
                // 자바 기본 렌더링 엔진을 먼저 모두 실행(글자, 흰색 배경, 꺾쇠 버튼 인쇄 완료)
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(110, 90, 200)); // 보라색 테마 연동
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);

                g2.dispose();
            }
        };
        platformCombo.setOpaque(false);
        platformCombo.setBackground(Color.WHITE);
        platformCombo.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 5));
        platformCombo.setFocusable(false);
        platformCombo.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        platformCombo.setForeground(new Color(110, 90, 200));
        platformCombo.setPreferredSize(new Dimension(130, 32));

        // 드롭다운 목록 내부 디자인 동기화
        platformCombo.setRenderer(new javax.swing.DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus){
                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                c.setBackground(Color.WHITE);
                c.setForeground(new Color(110, 90, 200));
                c.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                c.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return c;
            }
        });

        //보라색 꺾쇠(V) 부착
        platformCombo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI(){
            @Override
            protected JButton createArrowButton(){
                JButton button = new JButton(){
                    @Override
                    protected void paintComponent(Graphics g){
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                        g2.setColor(new Color(110, 90, 200));
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
        platformCombo.addActionListener(e -> refreshParodyLibrary());

        JButton btnAddParody = new JButton("+ 새 패러디 추가") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(110, 90, 200));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnAddParody.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        btnAddParody.setForeground(new Color(110, 90, 200));
        btnAddParody.setFocusPainted(false); btnAddParody.setBorderPainted(false); btnAddParody.setContentAreaFilled(false);
        btnAddParody.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAddParody.setPreferredSize(new Dimension(130, 30));

        btnAddParody.addActionListener(e -> {
            AddNovelDialog dialog = new AddNovelDialog(mainFrame);
            dialog.setParodyMode(true);
            dialog.setVisible(true);
            Novel newParody = dialog.getResultNovel();
            if (newParody != null) {
                newParody.setGenre("패러디");
                parodyList.add(newParody);
                saveParodyMetadata();
                refreshParodyLibrary();
            }
        });

        String[] sortOption = {"최근 읽은 순", "제목순", "안 읽은 작품", "좋아요 순"};
        sortCombo = new JComboBox<>(sortOption){
            @Override
            protected void paintComponent(Graphics g){
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                //2. 테두리 드로잉(소프트 그레이)
                g2.setColor(new Color(225, 228, 232));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);

                g2.dispose();

            }
        };
        //내부 렌더링 동기화를 위해 불투명 속성을 소거, 날카로운 기본 각진 선을 제거
        sortCombo.setOpaque(false);
        sortCombo.setBackground(Color.WHITE);
        sortCombo.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 5));
        sortCombo.setFocusable(false);
        sortCombo.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        sortCombo.setForeground(new Color(60, 65, 75));
        sortCombo.setPreferredSize(new Dimension(135, 32));

        sortCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                c.setBackground(Color.WHITE);
                c.setForeground(new Color(60, 65, 75)); // 선택 글자색 다크 차콜 그레이
                c.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
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

        sortCombo.addActionListener(e -> refreshParodyLibrary());     //선택 시 리프레시 트리거

        // 검색창 가두리(취소 버튼 슬롯 레이아웃 조립)
        JPanel searchContainer = new JPanel(new BorderLayout(6, 0));
        searchContainer.setBackground(Color.WHITE);

        searchField = new JTextField("제목, 작가, 태그 검색...", 12){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                g2.setColor(new Color(225, 228, 232));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        searchField.setOpaque(false);
        searchField.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        searchField.setForeground(Color.GRAY);
        searchField.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        searchField.addActionListener(e -> refreshParodyLibrary());

        JButton btnCancelSearch = new JButton("취소");
        btnCancelSearch.setPreferredSize(new Dimension(30, 30));
        btnCancelSearch.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        btnCancelSearch.setForeground(new Color(110, 90, 200));
        btnCancelSearch.setFocusPainted(false);
        btnCancelSearch.setBorderPainted(false);
        btnCancelSearch.setContentAreaFilled(false);
        btnCancelSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelSearch.setMargin(new java.awt.Insets(0, 0, 0, 0));
        btnCancelSearch.setVisible(false);

        searchContainer.setPreferredSize(new Dimension(195, 30));
        searchContainer.add(searchField, BorderLayout.CENTER);
        searchContainer.add(btnCancelSearch, BorderLayout.EAST);

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

        btnCancelSearch.addActionListener(e -> {
            searchField.setForeground(Color.GRAY);
            searchField.setText("제목, 작가, 태그 검색...");
            refreshParodyLibrary();
            btnCancelSearch.setVisible(false);
            searchContainer.revalidate();
            searchContainer.repaint();
        });

        JLabel lblSearchInfo = new JLabel("ⓘ");
        lblSearchInfo.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblSearchInfo.setForeground(new Color(120, 130, 140));
        lblSearchInfo.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JWindow tipWindow = new JWindow(mainFrame);
        tipWindow.setBackground(new Color(0, 0, 0, 0));

        JPanel tipContainer = new JPanel(){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(248, 249, 251));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                g2.setColor(new Color(220, 225, 230));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
            }
        };
        tipContainer.setLayout(new BoxLayout(tipContainer, BoxLayout.Y_AXIS));
        tipContainer.setOpaque(false);
        tipContainer.setBorder(BorderFactory.createEmptyBorder(12 ,14, 12, 14));

        String[] infoLines = {
                "보관함 검색 팁 안내",
                "",
                "ㆍ@작가이름 (ex: @홍길동)",
                "ㆍ#키워드 (ex: #힐링물),",
                "ㆍ제목만 입력(ex: 콩쥐밭쥐)",
                "",
                "※ 부분 일치 검색 지원."
        };

        for(int i=0; i<infoLines.length; i++){
            JLabel lineLabel = new JLabel(infoLines[i]);
            if(i==0){
                lineLabel.setFont(new Font("맑은 고딕", Font.BOLD, 13));
                lineLabel.setForeground(new Color(110, 90, 200));
            } else if(infoLines[i].contains("부분 일치")){
                lineLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
                lineLabel.setForeground(new Color(130, 130, 130));
            } else{
                lineLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                lineLabel.setForeground(new Color(60, 60, 60));
            }
            tipContainer.add(lineLabel);
        }
        tipWindow.add(tipContainer);
        tipWindow.pack();

        lblSearchInfo.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e){
                //모니터 기준 ⓘ버튼의 실제 절대 위치 추출
                Point origin = lblSearchInfo.getLocationOnScreen();

                int targetX = origin.x + lblSearchInfo.getWidth() - tipWindow.getWidth();
                int targetY = origin.y + lblSearchInfo.getHeight()+4;
                tipWindow.setLocation(targetX, targetY);
                tipWindow.setVisible(true);
            }
            @Override
            public void mouseExited(MouseEvent e){
                tipWindow.setVisible(false);
            }
        });
        rightButtonGroup.add(platformCombo);
        rightButtonGroup.add(btnAddParody);
        rightButtonGroup.add(sortCombo);
        rightButtonGroup.add(searchContainer);
        rightButtonGroup.add(lblSearchInfo);

        toolbarPanel.add(leftTitleGroup, BorderLayout.WEST);
        toolbarPanel.add(rightButtonGroup, BorderLayout.EAST);

        //상단 툴바와 태바를 묶어줄 래퍼 패널
        JPanel topWrapperPanel = new JPanel(new BorderLayout());
        topWrapperPanel.setBackground(Color.WHITE);
        topWrapperPanel.add(toolbarPanel, BorderLayout.NORTH);

        //상태 탭바 생성
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
        add(topWrapperPanel, BorderLayout.NORTH);
    }

    // 2. [내 서재] 본문 마스터 패널 스크롤 사양 완벽 동기화
    private void initListBody() {
        libraryGridPanel = new JPanel();
        libraryGridPanel.setBackground(new Color(248, 250, 252));
        libraryGridPanel.setLayout(new GridBagLayout()); // 바둑판식 Layout 배치 가동

        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.setBackground(new Color(248, 250, 252));
        gridWrapper.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JPanel innerAlignPanel = new JPanel(new BorderLayout());
        innerAlignPanel.setBackground(Color.WHITE);
        innerAlignPanel.add(libraryGridPanel, BorderLayout.WEST);
        gridWrapper.add(innerAlignPanel, BorderLayout.NORTH);

        JScrollPane libraryScrollPane = new JScrollPane(gridWrapper);
        libraryScrollPane.setBackground(new Color(248, 250, 252));
        libraryScrollPane.getViewport().setBackground(new Color(248, 250, 252));
        libraryScrollPane.setBorder(BorderFactory.createEmptyBorder());
        libraryScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        //빈 여백 바닥 클릭 시 검색창 포커스 날려서 깜빡임 종료
        this.setFocusable(true);
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
            }
        });
        libraryGridPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
            }
        });
        libraryScrollPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
            }
        });

        add(libraryScrollPane, BorderLayout.CENTER);
    }

    // 3. [내 서재]와 동일 기조의 실시간 필터 및 카드 소트 리렌더링 엔진
    public void refreshParodyLibrary() {
        if (libraryGridPanel == null) return;
        libraryGridPanel.removeAll();
        cardCount = 0;

        String selectedPlatform = (platformCombo != null) ? (String) platformCombo.getSelectedItem() : "전체 플랫폼";
        String selectedSort = (sortCombo != null) ? (String) sortCombo.getSelectedItem() : "최근 읽은 순";
        String searchText = searchField.getText().trim().toLowerCase();
        if (searchText.equals("제목, 작가, 태그 검색...")) searchText = "";

        ArrayList<Novel> baseFilteredList = new ArrayList<>();
        int countAll = 0, countOngoing = 0, countCompleted = 0, countHiatus = 0;

        for (Novel novel : parodyList) {
            boolean matchesPlatform = selectedPlatform.equals("전체 플랫폼") || novel.getPlatform().contains(selectedPlatform);

            boolean matchesSearch = true;
            if (!searchText.isEmpty()) {
                if (searchText.contains("#")) {
                    String[] tokens = searchText.split("\\s+");
                    String novelKeywords = novel.getKeywords() != null ? novel.getKeywords().toLowerCase() : "";
                    for (String token : tokens) {
                        if (token.startsWith("#") && token.length() > 1) {
                            String cleanKey = token.substring(1).replace(",", "").trim();
                            if (!cleanKey.isEmpty() && !novelKeywords.contains(cleanKey)) { matchesSearch = false; break; }
                        }
                    }
                } else if (searchText.startsWith("@")) {
                    String cleanAuthor = searchText.substring(1).trim();
                    String novelAuthor = novel.getAuthor() != null ? novel.getAuthor().toLowerCase() : "";
                    if (!cleanAuthor.isEmpty() && !novelAuthor.contains(cleanAuthor)) matchesSearch = false;
                } else {
                    matchesSearch = novel.getTitle().toLowerCase().contains(searchText);
                }
            }
            boolean matchesMenu = true;
            if(selectedSort.equals("좋아요 순")){
                matchesMenu = novel.isFavorite();
            } else if(selectedSort.equals("안 읽은 작품")){
                if(novel.getLastReadDate().equals("기록 없음")){
                    matchesMenu = true;
                } else{
                    int totalCh = 0;
                    int currentCh = 1;
                    File dir = new File(novel.getFolderPath());
                    if(dir.exists() && dir.isDirectory()){
                        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".txt") && !name.toLowerCase().equals("bookmark.txt"));
                        if(files != null) totalCh = files.length;
                    }
                    File bookmarkFile = new File(novel.getFolderPath() + File.separator + "bookmark.txt");
                    if(bookmarkFile.exists()){
                        try(BufferedReader br = new BufferedReader(new FileReader(bookmarkFile))){
                            String line = br.readLine();
                            if(line != null) currentCh = Integer.parseInt(line.trim());
                        } catch(Exception e) { currentCh = 1;}
                    }
                    if(totalCh == 0) totalCh = 1;
                    matchesMenu = (currentCh < totalCh);
                }
            }

            // 3가지 필터망(플랫폼, 검색, 필터메뉴)을 모두 통과한 것만 수집
            if(matchesPlatform && matchesSearch && matchesMenu){
                baseFilteredList.add(novel);
                countAll++;
                if(novel.isCompleted()) countCompleted++;
                else if(novel.isHiatus()) countHiatus++;
                else countOngoing++;
            }
        }

        if(btnTabAll != null) btnTabAll.setText("전체 (" + countAll + ")");
        if(btnTabOngoing != null) btnTabOngoing.setText("연재중 (" + countOngoing + ")");
        if(btnTabCompleted != null) btnTabCompleted.setText("완결 (" + countCompleted + ")");
        if(btnTabHiatus != null) btnTabHiatus.setText("연재중단 (" + countHiatus + ")");

        ArrayList<Novel> finalFilteredList = new ArrayList<>();
        for(Novel novel : baseFilteredList){
            boolean matchesTab = true;
            if(currentStatusTab.equals("완결")) matchesTab = novel.isCompleted();
            else if(currentStatusTab.equals("연재중단")) matchesTab = novel.isHiatus();
            else if(currentStatusTab.equals("연재중")) matchesTab = !novel.isCompleted() && !novel.isHiatus();

            if(matchesTab) finalFilteredList.add(novel);
        }

        if (lblTotalCounter != null){
            if(selectedSort.equals("좋아요 순")){
                lblTotalCounter.setText("좋아요 작품: " + finalFilteredList.size() + "개");
            } else if(selectedSort.equals("안 읽은 작품")){
                lblTotalCounter.setText("안 읽은 작품: " + finalFilteredList.size() + "개");
            } else{
                lblTotalCounter.setText("작품 수: " + finalFilteredList.size() + "개");
            }
        }

        // 소트 알고리즘
        if (selectedSort.equals("좋아요 순")) {
            Collections.sort(finalFilteredList, (n1, n2) -> Long.compare(n2.getFavoriteTimestamp(), n1.getFavoriteTimestamp()));
        } else if (selectedSort.equals("최근 읽은 순")) {
            Collections.sort(finalFilteredList, (n1, n2) -> {
                String d1 = n1.getLastReadDate(); String d2 = n2.getLastReadDate();
                if (d1.equals("기록 없음") && d2.equals("기록 없음")) return 0;
                if (d1.equals("기록 없음")) return 1; if (d2.equals("기록 없음")) return -1;
                return d2.compareTo(d1);
            });
        } else if (selectedSort.equals("제목순")) {
            Collections.sort(finalFilteredList, (n1, n2) -> n1.getTitle().compareTo(n2.getTitle()));
        }

        // 렌더링 카드 주입
        for (Novel novel : finalFilteredList) {
            addParodyCard(novel);
        }

        libraryGridPanel.revalidate();
        libraryGridPanel.repaint();
    }

    private void addParodyCard(Novel novel){
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
                //ImageIO를 사용하여 이미지를 동기식으로 로드
                File coverFile = new File(novel.getCoverPath());
                java.awt.image.BufferedImage srcImg = javax.imageio.ImageIO.read(coverFile);

                //2. 가로세로 비율 유지를 위한 정밀 좌표 연산(종횡비 보존)
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
            } catch(Exception e){
                lblCover.setText("IMAGE ERROR");
            }
        } else{
            //이미지가 없으면 글자로 대체
            lblCover.setText("NO COVER IMAGE");
            lblCover.setFont(new Font("맑은 고딕", Font.BOLD, 10));
            lblCover.setForeground(new Color(160, 170, 185));

            //텍스트 문장이 책 아이콘 하단에 알맞게 배치되도록 수직 텍스트 위치 가이드 기조 하향
            lblCover.setVerticalTextPosition(SwingConstants.BOTTOM);
            lblCover.setHorizontalTextPosition(SwingConstants.CENTER);
            lblCover.setIconTextGap(10);    //아이콘과 글자 사이의 안전 격리 마진
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
        lblTitle.setFont(new Font("맑은 고딕", Font.BOLD, 13));
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
        lblGenreAuthor.setFont(new Font("맑은 고딕", Font.PLAIN, 11));

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
        lblProgressText.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        lblProgressText.setForeground(currentCh >= totalCh ? new Color(0, 160, 160) : new Color(120, 125, 130));

        JLabel lblPercent = new JLabel(percent + "%");
        lblPercent.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        lblPercent.setForeground(new Color(0, 140, 140));

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
                detailPage.openDetailPage(novel, masterShelf);
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

    //로고 이미지를 렌더링하는 서포트 고용 엔진
    private JLabel createPlatformLogoLabel(String imagePath, String fallbackText){
        JLabel lblLogo = new JLabel();
        File logoFile = new File(imagePath);
        if(logoFile.exists()){
            try{
                ImageIcon originalIcon = new ImageIcon(imagePath);
                int width = (int) ((double) originalIcon.getIconWidth() * (14.0/originalIcon.getIconHeight()));
                Image scaledImg = originalIcon.getImage().getScaledInstance(width > 50 ? 50 : width, 14, Image.SCALE_SMOOTH);
                lblLogo.setIcon(new ImageIcon(scaledImg));
                lblLogo.setToolTipText(fallbackText);
            } catch(Exception e){
                lblLogo.setText("[" + fallbackText + "]");
                lblLogo.setFont(new Font("맑은 고딕", Font.BOLD, 10));
                lblLogo.setForeground(new Color(255, 120, 0));
            }
        } else{
            lblLogo.setText("[" + fallbackText + "]");
            lblLogo.setFont(new Font("맑은 고딕", Font.BOLD, 10));
            lblLogo.setForeground(new Color(255, 120, 0));
        }
        return lblLogo;
    }

    // 5. 패러디 전용 데이터 백엔드 파일 I/O 시스템 제어
    public void saveParodyMetadata() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(PARODY_DATA_FILE))) {
            for (Novel novel : parodyList) {
                String safeDescription = (novel.getDescription() != null) ? novel.getDescription().replace("\n", "[NEWLINE]") : "";
                String line = novel.getTitle() + "|" + novel.getAuthor() + "|" + novel.getGenre() + "|" +
                        novel.getPlatform() + "|" + novel.getFolderPath() + "|" + novel.getCoverPath() + "|" +
                        novel.getKeywords() + "|" + safeDescription + "|" + novel.getLastReadDate() + "|" +
                        novel.isFavorite() + "|" + novel.isCompleted() + "|" + novel.getCreatedDate() +"|" + novel.isHiatus();
                bw.write(line); bw.newLine();
            }
        } catch (Exception e) { System.out.println("패러디 세이브 실패: " + e.getMessage()); }
    }

    public void loadParodyMetadata() {
        File file = new File(PARODY_DATA_FILE);
        if (!file.exists()) return;
        parodyList.clear(); cardCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split("\\|", -1);
                if (d.length >= 10) {
                    String restoredDescription = d[7].replace("[NEWLINE]", "\n");
                    Novel novel = new Novel(d[0], d[1], d[2], d[3], d[4], d[5], d[6], restoredDescription, d[8], Boolean.parseBoolean(d[9]));
                    if (d.length >= 11) novel.setCompleted(Boolean.parseBoolean(d[10]));
                    if (d.length >= 12) novel.setCreatedDate(d[11]);
                    if(d.length >= 13) novel.setHiatus(Boolean.parseBoolean(d[12]));
                    parodyList.add(novel);
                }
            }
            refreshParodyLibrary();
        } catch (Exception e) { System.out.println("패러디 로드 오류: " + e.getMessage()); }
    }

    // 1. 상세창에서 패러디 소설 삭제 실행 시 호출
    public void deleteNovel(Novel novel){
        if(parodyList.remove(novel)){
            saveParodyMetadata();
            refreshParodyLibrary();
        }
    }

    // 2. 상세창에서 작가 이름 클릭 시 패러디 서재 내에서 검색 필터 가동
    public void searchByAuthor(String authorName){
        if(searchField != null){
            searchField.setText(authorName);
            searchField.setForeground(Color.BLACK);
            refreshParodyLibrary();
        }
    }

    // 3. 상세창에서 뒤로가기 클릭 시 (또는 삭제 완료 시) 메뉴로 복귀하는 트리거
    public void triggerMyLibraryMenu(){
        if(masterShelf != null){
            masterShelf.currentMenu = "패러디 서재";
            refreshParodyLibrary();
        }
    }

    //탭 버튼 생성 및 상태 동기화 헬퍼 메서드
    private JButton createTabButton(String baseTitle){
        JButton btn = new JButton(baseTitle){
            @Override
            protected void paintComponent(Graphics g){
                super.paintComponent(g);
                if(getText().startsWith(currentStatusTab + " (")){
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(110, 90, 200));
                    g2.fillRect(0, getHeight()-2, getWidth(), 2);
                    g2.dispose();
                }
            }
        };
        btn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        btn.addActionListener(e -> {
            currentStatusTab = baseTitle;
            updateTabStyles();
            refreshParodyLibrary();
        });
        return btn;
    }

    private void updateTabStyles(){
        if(btnTabAll == null) return;
        JButton[] tabs = {btnTabAll, btnTabOngoing, btnTabCompleted, btnTabHiatus};
        for(JButton btn : tabs){
            if(btn.getText().startsWith(currentStatusTab + " (")){
                btn.setForeground(new Color(110, 90, 200));
            } else{
                btn.setForeground(new Color(140, 145, 155));
            }
            btn.repaint();
        }
    }

    public void clearAllData() {
        parodyList.clear();       // 패러디 패널의 리스트 변수명에 맞게 수정
        saveParodyMetadata();    // 비어있는 상태로 파일 저장
        refreshParodyLibrary();  // UI 새로고침
    }
}