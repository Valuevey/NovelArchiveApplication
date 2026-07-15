import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

public class ShortStoryPanel extends JPanel {
    private JFrame mainFrame;
    private Object parentShelf;     //메인 서재(BookShelfPage) 참조 보관용 변수

    //상단 필터 제어 컴포넌트
    private JComboBox<String> sortCombo;
    private JTextField searchField;
    private JTextField leftWorkSearchField;     //좌측 원작 목록 전용 검색창
    private JLabel lblTotalCounter;

    //좌우 분할 구조 레이아웃용 패널 멤버 변수
    private JPanel categoryAnchorPanel;     //좌측: 원작 검색 및 앵커 리스트
    private JPanel snippetListContainer;    //우측: 스니펫 타일 리스트 스크롤 패널

    // [단편/썰] 전용 데이터 관리 멤버 변수 및 파일 인프라 구축
    private ArrayList<Snippet> snippetList = new ArrayList<>();             //shorts_data.txt 보정치 기록 데이터
    private ArrayList<String> categoryList = new ArrayList<>();             //하드디스크 스캔 기반 원작 목록


    private final String SHORTS_DATA_FILE = "C:\\novel\\shorts_data.txt";    //격리 저장 파일 경로
    private final String WORK_LINKS_FILE = "C:\\novel\\work_links.txt";     //원작별 연결 폴더 경로 저장용 파일 명세

    private java.util.HashMap<String, String> workFolderMap = new java.util.HashMap<>();    //메모리 적재용 원적-폴더 매핑 테이블
    private String selectedCategory = "전체보기";               //선택된 좌측 원작 분류 필터 락
    private String selectedTab = "전체";
    private int currentPage = 1;
    private final int PAGE_SIZE = 10;

    private ArrayList<String> hiddenCategoryList = new ArrayList<>();

    private final java.util.Map<String, Image> bannerCoverImageCache = new java.util.HashMap<>();

    private JButton btnPlatformSelect;          // 커스텀 드롭다운 버튼
    private String selectedPlatform = "전체 플랫폼"; // 현재 선택된 플랫폼 저장 변수
    private final String PLATFORMS_FILE = "C:\\novel\\platforms.txt";   // 단편/썰 서재 플랫폼
    private ArrayList<String> platformItems = new ArrayList<>();

    //좌측 원작 목록 페이징 제어 변수(초기 7개 노출 후 더보기)
    private int visibleCategoryCount = 7;

    public ShortStoryPanel(JFrame mainFrame, Object parentShelf){
        this.mainFrame = mainFrame;
        this.parentShelf = parentShelf;

        //메인 서재 구조의 톤앤매너와 일치하도록 BorderLayout 기반 셋업
        setLayout(new BorderLayout(0, 0));
        setBackground(Color.WHITE);

        //UI 컴포넌트 초기화 및 분할 레이아웃
        initTopFilterBar();
        initMainContentSplitLayout();

        //시스템 기동 시 데이터 세이브 파일 선행 복원 로드
        loadShortsData();

        // 재시작 시 폴더 연결 정보를 불러옴
        loadWorkLinksData();
        loadPlatformsData();
    }

    // 1. 상단 필터, 정렬 검색 컴포넌트 라인 UI 조립 및 이벤트 바인딩
    private void initTopFilterBar() {
        JPanel topPanel = new JPanel(new BorderLayout(20, 0));
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));

        // 1. 좌측: 제목, 작품 수 뱃지
        JPanel leftTitleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftTitleGroup.setOpaque(false);

        JLabel lblPageTitle = new JLabel("단편/썰 서재");
        lblPageTitle.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        lblPageTitle.setForeground(new Color(25, 30, 40));

        // 작품 수 뱃지
        lblTotalCounter = new JLabel("작품 수: 0개"){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UiStyle.COLOR_MENU_HIGHLIGHT_BG);
                g2.fillRoundRect(0, 0, getWidth(),getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblTotalCounter.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        lblTotalCounter.setForeground(UiStyle.COLOR_ACCENT);
        lblTotalCounter.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        leftTitleGroup.add(lblPageTitle);
        leftTitleGroup.add(lblTotalCounter);

        // 2. 우측: 필터버튼, 검색창
        JPanel rightControlGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightControlGroup.setOpaque(false);

        // 플랫폼 버튼
        btnPlatformSelect = new JButton("전체 플랫폼 ▼") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(0, 160, 160));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnPlatformSelect.setFont(UiStyle.FONT_BOLD_12);
        btnPlatformSelect.setForeground(UiStyle.COLOR_ACCENT);
        btnPlatformSelect.setContentAreaFilled(false);
        btnPlatformSelect.setBorderPainted(false);
        btnPlatformSelect.setPreferredSize(new Dimension(130, 32));
        btnPlatformSelect.setFocusPainted(false);
        btnPlatformSelect.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 커스텀 팝업 메뉴 (드롭다운) 이벤트 바인딩
        btnPlatformSelect.addActionListener(e -> {
            JPopupMenu popup = new JPopupMenu();
            popup.setBackground(Color.WHITE);
            popup.setBorder(BorderFactory.createLineBorder(new Color(170, 175, 180), 1));

            // 메뉴 아이템 공통 스타일 적용을 위한 헬퍼(디자인 엔진)
            java.util.function.Consumer<JMenuItem> styleMenuItem = (item) -> {
                item.setFont(UiStyle.FONT_PLAIN_13);
                item.setForeground(UiStyle.COLOR_ACCENT);
                item.setBackground(Color.WHITE);
                item.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

                // 마우스 오버 시 기본 파란색 대신 연한 청록빛 배경으로 부드럽게 변경
                item.getModel().addChangeListener(ev -> {
                    ButtonModel model = item.getModel();
                    item.setBackground(model.isArmed() ? new Color(240, 248, 248) : Color.WHITE);
                });
            };

            // [전체 플랫폼] 기본 메뉴
            JMenuItem itemAll = new JMenuItem("전체 플랫폼");
            styleMenuItem.accept(itemAll);
            itemAll.addActionListener(ev -> {
                selectedPlatform = "전체 플랫폼";
                btnPlatformSelect.setText("전체 플랫폼 ▼");
                renderRightSnippetList();
            });
            popup.add(itemAll);

            // [개별 플랫폼] 동적 렌더링
            for (String p : platformItems) {
                JMenuItem item = new JMenuItem(p);
                styleMenuItem.accept(item);
                item.addActionListener(ev -> {
                    selectedPlatform = p;
                    btnPlatformSelect.setText(p + " ▼");
                    renderRightSnippetList();
                });
                popup.add(item);
            }

            // 투박한 3D 입체선 대신 1픽셀짜리 깔끔한 실선 구분선 생성
            popup.add(new JPopupMenu.Separator() {
                @Override
                public void paintComponent(Graphics g) {
                    g.setColor(new Color(230, 235, 240));
                    g.drawLine(0, 0, getWidth(), 0);
                }
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(0, 1);
                }
            });

            // [플랫폼 관리] 특수 메뉴 (이모티콘 제거, 텍스트 전용)
            JMenuItem itemManage = new JMenuItem("플랫폼 관리");
            itemManage.setFont(UiStyle.FONT_PLAIN_13);
            itemManage.setForeground(new Color(110, 115, 125)); // 일반 메뉴와 구분되도록 회색빛 적용
            itemManage.setBackground(new Color(250, 252, 253));
            itemManage.setOpaque(true);
            itemManage.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            itemManage.getModel().addChangeListener(ev -> {
                ButtonModel model = itemManage.getModel();
                itemManage.setBackground(model.isArmed() ? new Color(240, 242, 245) : new Color(250, 252, 253));
            });
            itemManage.addActionListener(ev -> showPlatformManagerDialog());
            popup.add(itemManage);

            // 드롭다운 메뉴의 가로 너비를 클릭한 버튼의 너비와 완벽하게 일치시킴
            popup.setPreferredSize(new Dimension(btnPlatformSelect.getWidth(), popup.getPreferredSize().height));

            // 버튼 바로 아래에 팝업 띄우기
            popup.show(btnPlatformSelect, 0, btnPlatformSelect.getHeight());
        });

        // 정렬 콤보박스
        sortCombo = new JComboBox<>(new String[]{"최신순", "제목순", "글자수 순"}) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 소프트 그레이 테두리
                g2.setColor(UiStyle.COLOR_BORDER_GRAY);
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
                g2.dispose();
            }
        };

        sortCombo.setOpaque(false);
        sortCombo.setBackground(Color.WHITE);
        sortCombo.setPreferredSize(new Dimension(135, 32));
        sortCombo.setFont(UiStyle.FONT_BOLD_12);
        sortCombo.setForeground(new Color(60, 65, 75));
        sortCombo.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 5));
        sortCombo.setFocusable(false);

// [핵심] 내 서재와 동일한 꺽쇠 UI 적용
        sortCombo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
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

// [핵심] 내 서재와 동일한 리스트 렌더러 적용 (선택 시 배경색 강제 변경 방지)
        sortCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                c.setBackground(isSelected ? new Color(240, 240, 240) : Color.WHITE); // 선택 시 연한 회색
                c.setForeground(new Color(60, 65, 75));
                c.setFont(UiStyle.FONT_PLAIN_12);
                c.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return c;
            }
        });

        sortCombo.addActionListener(e -> renderRightSnippetList());

        JPanel rightSearchGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 16));
        rightSearchGroup.setOpaque(false);

        searchField = new JTextField("제목, 작가, 태그 검색...", 15) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 배경 페인팅 (흰색 둥근 사각형)
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                // 테두리 드로잉 (연한 회색)
                g2.setColor(UiStyle.COLOR_BORDER_GRAY);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        searchField.setOpaque(false);
        searchField.setPreferredSize(new Dimension(195, 32));
        searchField.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        searchField.setFont(UiStyle.FONT_PLAIN_12);
        searchField.setForeground(Color.GRAY);

        // 안쪽 여백 설정 (텍스트가 둥근 테두리에 너무 붙지 않게 함)
        searchField.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        searchField.addFocusListener(new java.awt.event.FocusListener() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().equals("제목, 작가, 태그 검색...")) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().trim().isEmpty()) {
                    searchField.setForeground(Color.GRAY);
                    searchField.setText("제목, 작가, 태그 검색...");
                }
            }
        });

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                renderRightSnippetList();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                renderRightSnippetList();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                renderRightSnippetList();
            }
        });

        // 검색 안내 아이콘 (ⓘ)
        JLabel lblSearchInfo = new JLabel("ⓘ");
        lblSearchInfo.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblSearchInfo.setForeground(new Color(120, 130, 140));
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

        rightControlGroup.add(btnPlatformSelect);
        rightControlGroup.add(sortCombo);
        rightControlGroup.add(searchField);
        rightControlGroup.add(lblSearchInfo);

        topPanel.add(leftTitleGroup, BorderLayout.WEST);
        topPanel.add(rightControlGroup, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
    }

    // 원작 카테고리(좌측)와 슬림 리스트 뷰(우측)를 결합하는 메인 콘텐츠 분할 패널 조립
    private void initMainContentSplitLayout(){
        JPanel splitContainer = new JPanel(new BorderLayout(0,0));
        splitContainer.setBackground(Color.WHITE);

        //[좌측 파트]: 원작 카테고리 리스트 앵커 영역(가로 폭 210px 고정)
        JPanel leftAnchorWrapper = new JPanel(new BorderLayout());
        leftAnchorWrapper.setBackground(Color.WHITE);
        leftAnchorWrapper.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(225, 230, 235)));
        leftAnchorWrapper.setPreferredSize(new Dimension(220, 0));

        //좌측 서브 상단바 조립 패널(타이틀 + 작품추가 버튼 + 검색 바)
        JPanel leftHeaderContainer = new JPanel();
        leftHeaderContainer.setLayout(new BoxLayout(leftHeaderContainer, BoxLayout.Y_AXIS));
        leftHeaderContainer.setBackground(Color.WHITE);
        leftHeaderContainer.setBorder(BorderFactory.createEmptyBorder(15, 12, 10, 12));

        JPanel titleAndBtnRow = new JPanel(new GridBagLayout());
        titleAndBtnRow.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel lblLeftTitle = new JLabel("원작 목록");
        lblLeftTitle.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        lblLeftTitle.setForeground(new Color(40, 45, 55));

        gbc.gridx = 0;
        gbc.weightx = 1.0; // 타이틀이 가능한 많은 공간 차지
        gbc.anchor = GridBagConstraints.WEST;
        titleAndBtnRow.add(lblLeftTitle, gbc);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnGroup.setOpaque(false);

        JButton btnAddSort = new JButton("+ 원작 추가");
        btnAddSort.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        btnAddSort.setForeground(new Color(0, 150, 150));
        btnAddSort.setContentAreaFilled(false);
        btnAddSort.setBorderPainted(false);
        btnAddSort.setFocusPainted(false);
        btnAddSort.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAddSort.setMargin(new Insets(0, 0, 0, 0));

        btnAddSort.addActionListener(e -> {
            String newWorkName = JOptionPane.showInputDialog(mainFrame,
                    "새로 개설할 원작 웹소설 폴더의 이름을 입력하세요:",
                    "원작 단편 서재 개설", JOptionPane.PLAIN_MESSAGE);
            if(newWorkName != null && !newWorkName.trim().isEmpty()){
                String cleanName = newWorkName.trim();
                File baseDir = new File("C:\\novel\\novels\\short_stories\\" + cleanName);
                new File(baseDir, "단편").mkdirs();
                new File(baseDir, "썰").mkdirs();
                new File(baseDir, "연작").mkdirs();
                refreshShortStoryLibrary();
            }
        });

        //삭제 버튼 추가
        JButton btnDelete = new JButton("삭제");
        btnDelete.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        btnDelete.setForeground(new Color(200, 80, 80));
        btnDelete.setContentAreaFilled(false);
        btnDelete.setBorderPainted(false);
        btnDelete.setFocusPainted(false);
        btnDelete.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDelete.setMargin(new Insets(0, 0, 0, 0));

        btnDelete.addActionListener(e -> {
            if(selectedCategory.equals("전체보기")){
                JOptionPane.showMessageDialog(mainFrame, "삭제할 원작을 먼저 선택해 주세요.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(mainFrame,
                    "'" + selectedCategory + "' 원작을 목록에서 삭제하시겠습니까?\n\n※ 화면상에서만 사라지며 원본 폴더는 유지됩니다.",
                    "원작 삭제", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if(confirm == JOptionPane.YES_OPTION){
                hiddenCategoryList.add(selectedCategory);
                selectedCategory = "전체보기";
                refreshShortStoryLibrary();
                renderRightSnippetList();
            }
        });

        // 구분선 라벨
        JLabel lblDivider = new JLabel("|");
        lblDivider.setFont(UiStyle.FONT_PLAIN_11);
        lblDivider.setForeground(new Color(200, 205, 210));

        // 안내 아이콘 추가
        JLabel lblGuideInfo = new JLabel("ⓘ");
        lblGuideInfo.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblGuideInfo.setForeground(new Color(150, 155, 165));
        lblGuideInfo.setCursor(new Cursor(Cursor.HAND_CURSOR));

// 안내 팝업 설정 (기존 검색창 옆 안내창과 동일한 방식)
        JWindow guideWindow = new JWindow(mainFrame);
        guideWindow.setBackground(new Color(0, 0, 0, 0));
        JPanel tipContainer = createGuideTipContainer(); // 아래 별도 메서드 참조
        guideWindow.add(tipContainer);
        guideWindow.pack();

        lblGuideInfo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Point origin = lblGuideInfo.getLocationOnScreen();
                guideWindow.setLocation(origin.x + lblGuideInfo.getWidth(), origin.y);
                guideWindow.setVisible(true);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                guideWindow.setVisible(false);
            }
        });

        btnGroup.add(btnAddSort);
        btnGroup.add(lblDivider);
        btnGroup.add(btnDelete);
        btnGroup.add(Box.createHorizontalStrut(5)); // 간격 확보
        btnGroup.add(lblGuideInfo);

        // 3. 타이틀과 버튼 그룹 배치
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        titleAndBtnRow.add(lblLeftTitle, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        titleAndBtnRow.add(btnGroup, gbc);

        //실시간 원작 검색창 배치
        leftWorkSearchField = new JTextField();
        leftWorkSearchField.setFont(UiStyle.FONT_PLAIN_12);
        leftWorkSearchField.setPreferredSize(new Dimension(Integer.MAX_VALUE, 32));
        leftWorkSearchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        leftWorkSearchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyle.COLOR_BORDER_GRAY, 1, true),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));

        //포커스 플레이스홀더 주입
        leftWorkSearchField.setText("원작 검색...");
        leftWorkSearchField.setForeground(Color.GRAY);
        leftWorkSearchField.addFocusListener(new java.awt.event.FocusListener(){
            @Override
            public void focusGained(java.awt.event.FocusEvent e){
                if(leftWorkSearchField.getText().equals("원작 검색...")){
                    leftWorkSearchField.setText("");
                    leftWorkSearchField.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e){
                if(leftWorkSearchField.getText().trim().isEmpty()){
                    leftWorkSearchField.setForeground(Color.GRAY);
                    leftWorkSearchField.setText("원작 검색...");
                }
            }
        });

        leftWorkSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshShortStoryLibrary(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshShortStoryLibrary(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshShortStoryLibrary(); }
        });

        leftHeaderContainer.add(titleAndBtnRow);
        leftHeaderContainer.add(Box.createVerticalStrut(12));
        leftHeaderContainer.add(leftWorkSearchField);

        leftAnchorWrapper.add(leftHeaderContainer, BorderLayout.NORTH);

        //카테고리 항목들이 세로로 쌓일 내부 패널
        categoryAnchorPanel = new JPanel();
        categoryAnchorPanel.setLayout(new BoxLayout(categoryAnchorPanel, BoxLayout.Y_AXIS));
        categoryAnchorPanel.setBackground(Color.WHITE);

        JScrollPane categoryScroll = new JScrollPane(categoryAnchorPanel);
        categoryScroll.setBorder(null);
        categoryScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); // <─ [새로 추가]
        categoryScroll.getVerticalScrollBar().setUnitIncrement(12);

        leftAnchorWrapper.add(categoryScroll, BorderLayout.CENTER);

        //[우측 파트]: 표지 없는 슬림 타일 리스트 스크롤 컨테이너
        snippetListContainer = new JPanel();
        snippetListContainer.setLayout(new BoxLayout(snippetListContainer, BoxLayout.Y_AXIS));
        snippetListContainer.setBackground(Color.WHITE);
        snippetListContainer.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JScrollPane listScroll = new JScrollPane(snippetListContainer);
        listScroll.setBorder(null);
        listScroll.getViewport().setBackground(Color.WHITE);
        listScroll.getVerticalScrollBar().setUnitIncrement(16);

        splitContainer.add(leftAnchorWrapper, BorderLayout.WEST);
        splitContainer.add(listScroll, BorderLayout.CENTER);

        add(splitContainer, BorderLayout.CENTER);
    }

    //[코어 엔진 A] 하드디스크 원작 폴더 구조 스캔 및 좌측 카테고리 매핑 갱신
    public void refreshShortStoryLibrary(){
        if(categoryAnchorPanel == null) return;

        categoryList.clear();
        File baseDir = new File("C:\\novel\\novels\\short_stories");
        if(baseDir.exists() && baseDir.isDirectory()){
            File[] files = baseDir.listFiles();
            if(files != null){
                for(File f : files){
                    if(f.isDirectory() && (new File(f, "단편").exists() || new File(f, "썰").exists() || new File(f, "연작").exists())){
                        if(!hiddenCategoryList.contains(f.getName())){
                            categoryList.add(f.getName());
                        }
                    }
                }
            }
        }
        //정렬 순 가이드 지정
        Collections.sort(categoryList, (w1, w2) -> {
            String t1 = "0000-00-00 00:00:00";
            String t2 = "0000-00-00 00:00:00";

            //w1 원작 폴더가 가진 소설들 중 가장 최근에 읽은 날짜를 스캔
            for(Snippet sn : snippetList){
                if(sn.getParentWork().equals(w1) && sn.getLastReadDate() != null && !sn.getLastReadDate().equals("기록 없음")){
                    if(sn.getLastReadDate().compareTo(t1) > 0){
                        t1 = sn.getLastReadDate();
                    }
                }
            }
            // w2 원작 폴더가 가진 소설들 중 가장 최근에 읽은 날짜를 스캔
            for(Snippet sn : snippetList){
                if(sn.getParentWork().equals(w2) && sn.getLastReadDate() != null && !sn.getLastReadDate().equals("기록 없음")){
                    if(sn.getLastReadDate().compareTo(t2) > 0){
                        t2 = sn.getLastReadDate();
                    }
                }
            }
            //읽은 날짜가 더 최신인 원작이 위로 올라오도록 역순(내림차순)비교 연산 반환
            return t2.compareTo(t1);
        });
        categoryAnchorPanel.removeAll();

        //좌측 원작 검색 키워드 추출
        String query = leftWorkSearchField.getText().trim().toLowerCase();
        if(query.equals("원작 검색...")) query = "";

        ArrayList<String> filteredCategories = new ArrayList<>();
        if(query.isEmpty()){
            filteredCategories.add("전체보기");
            filteredCategories.addAll(categoryList);
        } else{
            for(String cat : categoryList){
                if(cat.toLowerCase().contains(query)){
                    filteredCategories.add(cat);
                }
            }
        }

        int limit = Math.min(visibleCategoryCount, filteredCategories.size());
        for(int i=0; i<limit; i++){
            addCategoryButton(filteredCategories.get(i));
        }

        //만약 남은 작품이 있다면 [더보기 V] 버튼 부착
        if(filteredCategories.size() > visibleCategoryCount){
            JButton btnMoreCat = new JButton("더보기 V");
            btnMoreCat.setFont(UiStyle.FONT_PLAIN_12);
            btnMoreCat.setForeground(Color.GRAY);
            btnMoreCat.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnMoreCat.setContentAreaFilled(false);
            btnMoreCat.setBorderPainted(false);
            btnMoreCat.setFocusPainted(false);
            btnMoreCat.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnMoreCat.addActionListener(e -> {
                visibleCategoryCount += 10;     //10개씩 슬라이스 개방 확대
                refreshShortStoryLibrary();
            });
            categoryAnchorPanel.add(Box.createVerticalStrut(10));
            categoryAnchorPanel.add(btnMoreCat);
        }
        categoryAnchorPanel.revalidate();
        categoryAnchorPanel.repaint();
    }

    private void addCategoryButton(String groupName){
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setMaximumSize(new Dimension(185, 48));
        card.setPreferredSize(new Dimension(185, 48));

        //선택 하이라이트 배경색 피드백 세팅
        if(selectedCategory.equals(groupName)){
            card.setBackground(new Color(240, 248, 248));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, new Color(0, 150, 150)),
                    BorderFactory.createEmptyBorder(5, 8,5, 12)
            ));
        } else{
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        }
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        //[좌측]: 표지 썸네일 박스 레이아웃 스캔 주입
        JPanel coverThumb = new JPanel(){
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 연결된 원작 폴더 경로로 내 서재에서 일치하는 Novel을 찾아 표지 경로 조회
                String workPath = workFolderMap.get(groupName);
                File coverImg = null;
                if (workPath != null && parentShelf instanceof BookShelfPage) {
                    BookShelfPage shelf = (BookShelfPage) parentShelf;
                    for (Novel novel : shelf.getNovelList()) {
                        if (novel.getFolderPath().equals(workPath)) {
                            String coverPath = novel.getCoverPath();
                            if (coverPath != null && !coverPath.isEmpty()) {
                                File f = new File(coverPath);
                                if (f.exists()) coverImg = f;
                            }
                            break;
                        }
                    }
                }

                if (coverImg != null) {
                    // 캐시에 있으면 파일을 다시 읽지 않고 즉시 사용
                    Image img = bannerCoverImageCache.get(coverImg.getAbsolutePath());
                    if (img == null) {
                        try {
                            img = javax.imageio.ImageIO.read(coverImg);
                            if (img != null) bannerCoverImageCache.put(coverImg.getAbsolutePath(), img);
                        } catch (IOException e) { /* 로딩 실패 시 기본색 */ }
                    }
                    if (img != null) {
                        g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                    }
                } else {
                    // 이미지가 없으면 기존 회색 박스
                    g2.setColor(new Color(235, 238, 242));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                    g2.setFont(new Font("맑은 고딕", Font.BOLD, 10));
                    g2.setColor(new Color(160, 165, 175));

                    g2.drawString("표지", 14, 32);
                }
                //만약 이미지 스캔 파일 구조가 구축되어 있다면 렌더 스펙 확장 지점
                g2.dispose();
            }
        };
        coverThumb.setPreferredSize(new Dimension(28, 38));
        coverThumb.setOpaque(false);

        JLabel lblName = new JLabel(groupName);
        lblName.setFont(new Font("맑은 고딕", selectedCategory.equals(groupName) ? Font.BOLD : Font.PLAIN, 12));
        lblName.setForeground(selectedCategory.equals(groupName) ? UiStyle.COLOR_ACCENT : new Color(60, 65, 75));

        //[우측]: 해당 원작이 품고 있는 전체 단편/썰 총 개수 스캔 카운터 엔진 호출
        int totalCount = countTotalFilesForWork(groupName);
        JLabel lblCount = new JLabel(totalCount > 0 ? String.format("%,d", totalCount) : "");
        lblCount.setFont(UiStyle.FONT_PLAIN_11);
        lblCount.setForeground(new Color(140, 150, 160));

        JPanel textGroup = new JPanel(new BorderLayout());
        textGroup.setOpaque(false);
        textGroup.add(lblName, BorderLayout.CENTER);
        textGroup.add(lblCount, BorderLayout.EAST);

        //전체보기 단추일 때는 표지 썸네일을 스킵 가드 처리
        if(!groupName.equals("전체보기")){
            card.add(coverThumb, BorderLayout.WEST);
        }
        card.add(textGroup, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selectedCategory = groupName;
                refreshShortStoryLibrary();     //하이라이트 반전 토글 및 리스트 리로드
                renderRightSnippetList();
            }
        });

        categoryAnchorPanel.add(card);
        categoryAnchorPanel.add(Box.createVerticalStrut(2));
    }

    //원작 명칭별 디스크 내부의 단편/썰/연작 통합 개수 계산기
    private int countTotalFilesForWork(String workName){
        if(workName.equals("전체보기")){
            int grandTotal = 0;
            for(String cat : categoryList){
                grandTotal += countTotalFilesForWork(cat);
            }
            return grandTotal;
        }
        int cnt = 0;
        String[] types = {"단편", "썰", "연작"};
        for(String t : types){
            File dir = new File("C:\\novel\\novels\\short_stories\\" + workName + "\\" + t);
            if(dir.exists() && dir.isDirectory()){
                File[] f = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".txt") && !n.toLowerCase().equals("bookmark.txt"));
                if(f != null) cnt += f.length;
            }
        }
        return cnt;
    }

    //[코어 엔진 B] 방안2 기반 디스크 실시간 파일 추출 및 컴팩트 타일 리스트 렌더링
    private void renderRightSnippetList(){
        if(snippetListContainer == null) return;
        snippetListContainer.removeAll();

        //1단계: 하드디스크 스캔 및 메모리 리스트 동기화(즐겨찾기 탭 분리 처리 추가)
        ArrayList<Snippet> currentScannedList = new ArrayList<>();

        //성능 최적화: 파일마다 snippetList 전체를 다시 훑지 않도록, 원작+유형+파일명 키로 미리 한 번만 매핑
        java.util.Map<String, Snippet> metadataLookup = new java.util.HashMap<>();
        for(Snippet sn : snippetList){
            if(sn.getFolderPath() != null){
                metadataLookup.put(sn.getParentWork() + "|" + sn.getSnippetType() + "|" + sn.getFolderPath(), sn);
            }
        }

        if(selectedTab.equals("즐겨찾기")){
            File favFile = new File("C:\\novel\\favorite_shorts.txt");
            if(favFile.exists()){
                try (BufferedReader br = new BufferedReader(new FileReader(favFile))){
                    String line;
                    while((line = br.readLine()) != null){
                        String[] parts = line.split("\\|");
                        if(parts.length >= 3){
                            String absPath = parts[0];
                            File f= new File(absPath);
                            if(f.exists()){
                                String type = "";
                                String work = "";
                                String seriesName = "";

                                File parent1 = f.getParentFile();
                                File parent2 = parent1.getParentFile();
                                File parent3 = parent2.getParentFile();

                                if(parent2.getName().equals("단편") || parent2.getName().equals("썰") || parent2.getName().equals("연작")){
                                    type = parent2.getName();
                                    work = parent3.getName();
                                    seriesName = parent1.getName();
                                } else if(parent1.getName().equals("단편") || parent1.getName().equals("썰") || parent1.getName().equals("연작")){
                                    type = parent1.getName();
                                    work = parent2.getName();
                                }

                                //원작 목록 필터링(전체 보기가 아니면 해당 원작만 출력)
                                if(!selectedCategory.equals("전체보기") && !selectedCategory.equals(work)) continue;

                                String title = parts[2].replace(".txt", "");
                                // 연작인데 제목에 시리즈명이 없다면 그때만 덧붙임
                                if(type.equals("연작") && !title.contains(seriesName)){
                                    title = "[" + seriesName + "] " + title;
                                }
                                int words = (int) (f.length() / 2.5);

                                Snippet meta = null;
                                if(type.equals("연작")) meta = metadataLookup.get(work + "|" + type + "|" + seriesName);
                                else meta = metadataLookup.get(work + "|" + type + "|" + parts[2]);

                                String author = meta != null ? meta.getAuthor() : "작자미상";
                                String plat = meta != null ? meta.getPlatform() : "기타";
                                String keys = meta != null ? meta.getKeywords() : "";
                                String desc = meta != null ? meta.getDescription() : "";

                                //즐겨찾기 항목은 폴더경로(folderPath) 변수에 '절대경로'를 하이재킹하여 보관
                                Snippet favSnip = new Snippet(title, author, plat, absPath, work, type, words, keys, desc, "기록 없음", true);
                                currentScannedList.add(favSnip);
                            }
                        }
                    }
                } catch (Exception ex) {}
            }
        } else{
            //기존 일반 탭 스캔 로직
            ArrayList<String> targetWorks = selectedCategory.equals("전체보기") ? categoryList : new ArrayList<>(Collections.singletonList(selectedCategory));

            String[] subType = {"단편", "썰", "연작"};
            for(String work : targetWorks){
                for(String type : subType){
                    File typeDir = new File("C:\\novel\\novels\\short_stories\\" + work + "\\" + type);
                    if(typeDir.exists() && typeDir.isDirectory()){

                        //연작일 때는 텍스트가 아니라 하위 서브 작품 폴더들을 탐색하도록 분기
                        if(type.equals("연작")){
                            File[] subWorks = typeDir.listFiles(File::isDirectory);
                            if(subWorks != null){
                                for(File sw : subWorks){
                                    File[] txtFiles = sw.listFiles((d, n) -> n.toLowerCase().endsWith(".txt") &&
                                            !n.toLowerCase().equals("bookmark.txt"));
                                    if(txtFiles != null && txtFiles.length > 0){
                                        //연작 그룹의 대표 파일(첫 번째 파일)을 기준으로 타이틀 및 기본 스펙 추출
                                        File repFile = txtFiles[0];
                                        String subWorkName = sw.getName();  //작품 폴더명이 연작 제목

                                        //해당 연작 묶음의 총 글자수 합산 연산
                                        int totalWorks = 0;
                                        for(File tf : txtFiles) totalWorks += (int)(tf.length() / 2.5);

                                        //메타데이터 매핑 매커니즘 바인딩(폴더 네임을 키값 파일명으로 우회 활용)
                                        Snippet meta = metadataLookup.get(work + "|연작|" + subWorkName);
                                        if(meta == null){
                                            meta = new Snippet(subWorkName, "작자미상", "포스타입", subWorkName, work,
                                                    "연작", totalWorks, "", "", "기록없음", false);
                                        } else{
                                            meta.setTitle(subWorkName);
                                            meta.setWordCount(totalWorks);
                                        }

                                        //내부 정보 추적용 보조 장치 바인딩(총 파일 개수 기록 보관용)
                                        // meta.setDescription(String.valueOf(txtFiles.length));
                                        currentScannedList.add(meta);
                                    }
                                }
                            }
                        }else{
                            File[] files = typeDir.listFiles((d, n) ->
                                    n.toLowerCase().endsWith(".txt") && !n.toLowerCase().equals("bookmark.txt")
                            );
                            if(files != null){
                                for(File f : files){
                                    String fileTitle = f.getName().replace(".txt", "");
                                    int words = (int) (f.length() / 2.5);       //바이트 용량 역산 글자수 유추

                                    Snippet meta = metadataLookup.get(work + "|" + type + "|" + f.getName());   //shorts_data.txt에 유저가 보정했던 작가/키워드 주석 스냅샷이 있는지 검증 매핑
                                    if(meta == null){   //기존 기록이 없는 태초의 신규 파일은 순정 기본값으로 세팅
                                        meta = new Snippet(fileTitle, "작자미상", "포스타입", f.getName(), work, type, words, "", "", "기록 없음", false);
                                    } else{
                                        meta.setTitle(fileTitle);   //실물크기 사양 최신 동기화
                                        meta.setWordCount(words);   //글자수나 제목 등 물리 변동 사양만 최신 파일 가이드라인으로 보정 업데이트
                                    }
                                    currentScannedList.add(meta);
                                }
                            }
                        }
                    }
                }
            }
        }

        //2단계: 상단 필터 및 다중 접두사 기호 기반(@작가, #태그) 정밀 필터링 검사
        String selectPlat = selectedPlatform;
        String userQuery = searchField.getText().trim().toLowerCase();
        if(userQuery.equals("제목, 작가, 태그 검색...")) userQuery = "";

        ArrayList<Snippet> filtered = new ArrayList<>();
        for(Snippet sn : currentScannedList){
            boolean pMatch = selectPlat.equals("전체 플랫폼") || sn.getPlatform().contains(selectPlat);

            //탭 메뉴 매칭 가드 연동
            boolean tMatch = true;
            if(selectedTab.equals("단편")){
                tMatch = sn.getSnippetType().equals("단편");
            } else if(selectedTab.equals("썰")){
                tMatch = sn.getSnippetType().equals("썰");
            }else if(selectedTab.equals("연작")){
                tMatch = sn.getSnippetType().equals("연작");
            } else if(selectedTab.equals("즐겨찾기")){
                tMatch = sn.isFavorite();       //소설 고유의 즐겨찾기 플래그 필터
            }

            boolean sMatch = true;
            if(!userQuery.isEmpty()){
                if(userQuery.contains("#")){
                    String[] tokens = userQuery.split("\\s+");
                    String snKeywords = sn.getKeywords() != null ? sn.getKeywords().toLowerCase() : "";
                    for(String token : tokens){
                        if(token.startsWith("#") && token.length() > 1){
                            String cleanKey = token.substring(1).trim();
                            if(!cleanKey.isEmpty() && !snKeywords.contains(cleanKey)){ sMatch = false; break; }
                        } else if(!token.startsWith("#") && !snKeywords.contains(token)){ sMatch = false; break; }
                    }
                } else if(userQuery.startsWith("@")){
                    String cleanAuthor = userQuery.substring(1).trim();
                    String snAuthor = sn.getAuthor() != null ? sn.getAuthor().toLowerCase() : "";
                    sMatch = !cleanAuthor.isEmpty() && snAuthor.contains(cleanAuthor);
                } else {
                    String snTitle = sn.getTitle().toLowerCase();
                    String snAuthor = sn.getAuthor().toLowerCase();
                    String snKeywords = sn.getKeywords().toLowerCase();
                    sMatch = snTitle.contains(userQuery) || snAuthor.contains(userQuery) || snKeywords.contains(userQuery);
                }
            }
            if(pMatch && tMatch && sMatch) filtered.add(sn);
        }

        //3단계: 정렬 엔진 세팅
        String selectSort = (String) sortCombo.getSelectedItem();
        if(selectSort.equals("제목순")){
            Collections.sort(filtered, (s1, s2) -> s1.getTitle().compareTo(s2.getTitle()));
        } else if(selectSort.equals("글자수 순")){
            Collections.sort(filtered, (s1, s2) -> Integer.compare(s2.getWordCount(), s1.getWordCount()));
        } else{
            //디스크 기본 파일 정렬 타임스탬프 기준 역순(최신 추가순)
            Collections.sort(filtered, (s1, s2) -> s2.getCreatedDate().compareTo(s1.getCreatedDate()));
        }

        lblTotalCounter.setText("작품 수: " + filtered.size() + "개");

        //메인 원작 와이드 배너 패널 빌드 구역
        JPanel headerBannerCard = new JPanel(new BorderLayout(18, 0));
        headerBannerCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        headerBannerCard.setPreferredSize(new Dimension(600, 80));
        headerBannerCard.setBackground(Color.WHITE);
        headerBannerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 235, 240), 1, true),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        // 1) 표지 공란 (연결된 원작 폴더가 있으면 실제 표지 이미지로 대체)
        JPanel bannerCover = new JPanel(){
            @Override
            protected void paintComponent(Graphics g){
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 연결된 원작 폴더 경로 확인
                // 연결된 원작 폴더 경로로 내 서재에서 일치하는 Novel을 찾아 표지 경로 조회
                String workPath = workFolderMap.get(selectedCategory);
                File coverImg = null;
                if (workPath != null && parentShelf instanceof BookShelfPage) {
                    BookShelfPage shelf = (BookShelfPage) parentShelf;
                    for (Novel novel : shelf.getNovelList()) {
                        if (novel.getFolderPath().equals(workPath)) {
                            String coverPath = novel.getCoverPath();
                            if (coverPath != null && !coverPath.isEmpty()) {
                                File f = new File(coverPath);
                                if (f.exists()) coverImg = f;
                            }
                            break;
                        }
                    }
                }

                if (coverImg != null) {
                    // 캐시에 있으면 파일을 다시 읽지 않고 즉시 사용
                    Image img = bannerCoverImageCache.get(coverImg.getAbsolutePath());
                    if (img == null) {
                        try {
                            img = javax.imageio.ImageIO.read(coverImg);
                            if (img != null) bannerCoverImageCache.put(coverImg.getAbsolutePath(), img);
                        } catch (IOException e) { /* 로딩 실패 시 기본색 */ }
                    }
                    if (img != null) {
                        g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                    }
                } else {
                    // 이미지가 없으면 기존 회색 박스
                    g2.setColor(new Color(235, 238, 242));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                    g2.setFont(new Font("맑은 고딕", Font.BOLD, 10));
                    g2.setColor(new Color(160, 165, 175));

                    g2.drawString("표지", 14, 32);
                }
                g2.dispose();
            }
        };
        bannerCover.setPreferredSize(new Dimension(46, 56));
        bannerCover.setOpaque(false);

        // 2) 원작 제목 컴포넌트
        JLabel lblBannerTitle = new JLabel(selectedCategory);
        lblBannerTitle.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        lblBannerTitle.setForeground(new Color(40, 45, 55));

        // 3) 작품 보러가기 버튼
        boolean isFolderLinked = workFolderMap.containsKey(selectedCategory) && !selectedCategory.equals("전체보기");

        JButton btnGoWork = new JButton("작품 보러가기"){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(isFolderLinked ? Color.WHITE : new Color(245, 246, 248));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                g2.setColor(isFolderLinked ? new Color(215, 220, 225) : new Color(230, 235, 240));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnGoWork.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        btnGoWork.setForeground(isFolderLinked ? new Color(70, 75, 85) : new Color(180, 185, 190)); //미연결 시 연한 회색
        btnGoWork.setPreferredSize(new Dimension(110, 32));
        btnGoWork.setContentAreaFilled(false);
        btnGoWork.setBorderPainted(false);
        btnGoWork.setFocusPainted(false);
        if(isFolderLinked) btnGoWork.setCursor(new Cursor(Cursor.HAND_CURSOR));

        //파일 탐색기로 실물 원작 폴더 개방 액션 바인딩
        btnGoWork.addActionListener(e -> {
            if(!isFolderLinked) return;
            String path = workFolderMap.get(selectedCategory);

            // parsentShelf를 BookShelfPage로 캐스팅하여 메인 서재 시스템에 접근
            if(parentShelf instanceof BookShelfPage){
                BookShelfPage shelf = (BookShelfPage) parentShelf;
                Novel targetNovel = null;

                //내 서재에서 등록된 소설 목록을 전수조사하여 폴더 경로가 일치하는 객체 발굴
                for(Novel novel : shelf.getNovelList()){
                    if(novel.getFolderPath().equals(path)){
                        targetNovel = novel;
                        break;
                    }
                }

                //일치하는 원작을 찾았다면 즉시 상세창 렌더링 엔진 가동
                if(targetNovel != null){
                    NovelDetailPage detailPage = new NovelDetailPage();
                    detailPage.openDetailPage(targetNovel, shelf);
                } else {
                    //서재에서 해당 작품이 삭제되었거나 경로가 꼬였을 때의 가드
                    JOptionPane.showMessageDialog(mainFrame,
                            "연결된 경로의 원작 소설을 [내 서재]에서 찾을 수 없습니다.\n해당 원작이 보관함에 정상적으로 등록되어 있는지 확인해 주세요",
                            "원작 연결 실패", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        //배너 우측 옵션 미니 메뉴 단추(︙) 조립 및 가공
        JLabel lblBannerOpt = new JLabel("︙", JLabel.CENTER);
        lblBannerOpt.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        lblBannerOpt.setForeground(new Color(150, 155, 165));
        lblBannerOpt.setPreferredSize(new Dimension(30, 32));
        if(!selectedCategory.equals("전체보기")){
            lblBannerOpt.setCursor(new Cursor(Cursor.HAND_CURSOR));
            lblBannerOpt.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem menuEdit = new JMenuItem("원작 서재 수정 (폴더 연결)");
                    JMenuItem menuDel = new JMenuItem("원작 서재 삭제");

                    //폴더 연결 수정 로직 바인딩
                    menuEdit.addActionListener(evt -> {
                        JFileChooser chooser = new JFileChooser("C:\\novel\\novels\\novel_list");
                        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        chooser.setDialogTitle("[" + selectedCategory +
                                "] 원작 소설 실물 폴더 지정");
                        int returnVal = chooser.showOpenDialog(mainFrame);
                        if(returnVal == JFileChooser.APPROVE_OPTION){
                            String selectedPath = chooser.getSelectedFile().getAbsolutePath();
                            workFolderMap.put(selectedCategory, selectedPath);
                            saveWorkLinksData();    //설정 백업 저장
                            renderRightSnippetList();   //배너 즉시 활성화 리렌더링
                        }
                    });

                    //기존 깜빡이던 삭제 핵심 로직을 안전한 팝업 내부로 전격 이식
                    menuDel.addActionListener(evt -> {
                        int confirm = JOptionPane.showConfirmDialog(mainFrame, "[" + selectedCategory + "] 원작 서재를 목록에서 삭제하시겠습니까?", "원작 삭제 가드", JOptionPane.YES_NO_OPTION);
                        if(confirm == JOptionPane.YES_OPTION) {
                            int fileCount = countTotalFilesForWork(selectedCategory);
                            if(fileCount > 0) {
                                hiddenCategoryList.add(selectedCategory);
                                JOptionPane.showMessageDialog(mainFrame, "내부에 원고 파일이 존재하므로 실물 폴더는 보존하고 목록에서만 은닉 처리했습니다.", "안내", JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                File targetDir = new File("C:\\novel\\novels\\short_stories\\" + selectedCategory);
                                deleteFolderRecursively(targetDir);

                                workFolderMap.remove(selectedCategory); saveWorkLinksData(); // 매핑 삭제
                                JOptionPane.showMessageDialog(mainFrame, "내부가 비어있는 원작 폴더이므로 실물 하드디스크에서 영구 삭제 마감했습니다.", "완료", JOptionPane.INFORMATION_MESSAGE);
                            }
                            selectedCategory = "전체보기";
                            refreshShortStoryLibrary();
                        }
                    });
                    popup.add(menuEdit);
                    popup.add(menuDel);
                    popup.show(lblBannerOpt, e.getX(), e.getY());
                }
            });
        }

        JPanel bannerLeftGroup = new JPanel(new BorderLayout(15, 0));
        bannerLeftGroup.setOpaque(false);
        bannerLeftGroup.add(bannerCover, BorderLayout.WEST);
        bannerLeftGroup.add(lblBannerTitle, BorderLayout.CENTER);

        JPanel bannerRightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        bannerRightGroup.setOpaque(false);
        bannerRightGroup.add(btnGoWork);
        bannerRightGroup.add(lblBannerOpt); //보러가기 단추와 옵션 기호를 나란히 배치

        headerBannerCard.add(bannerLeftGroup, BorderLayout.CENTER);
        headerBannerCard.add(bannerRightGroup, BorderLayout.EAST);

        //상단에 신설 배너 카드를 안착하고 세로 여백 스페이스 부여
        snippetListContainer.add(headerBannerCard);
        snippetListContainer.add(Box.createVerticalStrut(15));

        // 플랫 메뉴 탭 바(전체, 단편, 연작, 즐겨찾기) 조립
        JPanel tabMenuBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        tabMenuBar.setOpaque(false);
        tabMenuBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        String[] tabNames = {"전체", "단편", "썰", "연작", "즐겨찾기"};
        for(String tabName : tabNames){
            JButton tabBtn = new JButton(tabName){
                @Override
                protected void paintComponent(Graphics g){
                    super.paintComponent(g);
                    if(selectedTab.equals(tabName)){
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setColor(new Color(0, 150, 150));
                        g2.setStroke(new BasicStroke(2.5f));
                        g2.drawLine(2, getHeight() -2, getWidth()-2, getHeight()-2);
                        g2.dispose();
                    }
                }
            };
            tabBtn.setFont(new Font("맑은 고딕", selectedTab.equals(tabName) ? Font.BOLD : Font.PLAIN, 13));
            tabBtn.setForeground(selectedTab.equals(tabName) ? UiStyle.COLOR_ACCENT : new Color(110, 115, 125));
            tabBtn.setContentAreaFilled(false);
            tabBtn.setBorderPainted(false);
            tabBtn.setFocusPainted(false);
            tabBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            tabBtn.addActionListener(evt -> {
                selectedTab = tabName;
                currentPage = 1;
                renderRightSnippetList();   //탭 전격 교체 즉시 리렌더링 트리거
            });
            tabMenuBar.add(tabBtn);
        }
        snippetListContainer.add(tabMenuBar);
        snippetListContainer.add(Box.createVerticalStrut(5));

        //와이드 그리드 컬럼 인덱스 헤더 행 마운트 <-뭔 말이지
        JPanel tableHeaderRow = new JPanel(new GridBagLayout());
        tableHeaderRow.setBackground(new Color(250, 251, 252));
        tableHeaderRow.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(230, 235, 240)));

        GridBagConstraints hGbc = new GridBagConstraints();
        hGbc.fill = GridBagConstraints.HORIZONTAL;
        hGbc.weighty = 1.0;

        //컬럼 규칙 정의(구분: 70px, 제목: 남은 공간 전체, 작가: 100px, 플랫폼: 100px, 옵션: 40px)
        String[] colums = {"구분", "제목 / 소제목", "분량", "작가", "플랫폼", ""};
        int[] widths = {75, 0, 85, 110, 95, 35};

        for(int i=0; i<colums.length; i++){
            JLabel lblH = new JLabel(colums[i], i == 1 ? JLabel.LEFT : JLabel.CENTER);
            lblH.setFont(new Font("맑은 고딕", Font.BOLD, 11));
            lblH.setForeground(new Color(120, 125, 135));

            hGbc.gridx = i;
            if(widths[i] == 0){
                hGbc.weightx = 1.0; //제목 칸에 너비 가중치 전부 위임
            } else{
                hGbc.weightx = 0.0;
                lblH.setPreferredSize(new Dimension(widths[i], 26));
            }
            tableHeaderRow.add(lblH, hGbc);
        }
        JPanel headerRowWrapper = new JPanel(new BorderLayout());
        headerRowWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        headerRowWrapper.add(tableHeaderRow, BorderLayout.CENTER);

        snippetListContainer.add(headerRowWrapper);
        snippetListContainer.add(Box.createVerticalStrut(4));

        //페이징 범위 산출 수식 조립 구역
        int totalItems = filtered.size();
        int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);
        if(totalPages == 0 ) totalPages = 1;
        if(currentPage > totalPages) currentPage = totalPages;

        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalItems);


        //하단부 소설 로우 리스트 출력
        for(int i=startIndex; i<endIndex; i++){
            Snippet sn = filtered.get(i);

            JPanel row = new JPanel(new GridBagLayout());
            row.setBackground(Color.WHITE);
            row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(245, 246, 248)));
            row.setCursor(new Cursor(Cursor.HAND_CURSOR));

            //외부 클릭 시 검색창 포커스 아웃
            row.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if(searchField != null && searchField.hasFocus()){
                        categoryAnchorPanel.requestFocusInWindow(); // <─ [새로 추가]
                    }
                }
            });

            GridBagConstraints rGbc = new GridBagConstraints();
            rGbc.fill = GridBagConstraints.HORIZONTAL;
            rGbc.weighty = 1.0;

            // 구분 정렬 칸
            String typeText = sn.getSnippetType();
            String finalBadgeText = typeText;

            if(typeText.equals("연작")){
                boolean isFavItem = sn.getFolderPath().contains(":\\"); //절대경로 감지
                if(isFavItem){
                    //즐겨찾기 파일은 저장된 절대경로에서 파일들을 세어줌
                    File parentDir = new File(sn.getFolderPath()).getParentFile();
                    File[] txtFiles = parentDir.listFiles((d, n) ->
                            n.toLowerCase().endsWith(".txt") && !n.toLowerCase().equals("bookmark.txt"));
                    finalBadgeText = "연작 (" + (txtFiles != null ? txtFiles.length : 0) + ")";
                } else{
                    //연작인 경우 디스크에서 실시간으로 해당 폴더의 텍스트 파일 개수를 세어 뱃지에 출력
                    int fileCount = 0;
                    File seriesDir = new File("C:\\novel\\novels\\short_stories\\" + sn.getParentWork() + "\\연작\\" + sn.getFolderPath());

                    if(seriesDir.exists() && seriesDir.isDirectory()){
                        File[] txtFiles = seriesDir.listFiles((d, n) ->
                                n.toLowerCase().endsWith(".txt") && !n.toLowerCase().equals("bookmark.txt"));
                        if(txtFiles != null){
                            fileCount = txtFiles.length;
                        }
                    }
                    finalBadgeText = "연작 (" + fileCount + ")";
                }
            }

            //타입별 고유 테마 색상(단편:청록, 썰:오렌지, 연작:보라)
            Color bgTheme;
            Color fgTheme;
            if(typeText.equals("단편")){
                bgTheme = new Color(235, 247, 245); fgTheme = UiStyle.COLOR_ACCENT;
            } else if(typeText.equals("썰")){
                bgTheme = new Color(255, 243, 230); fgTheme = new Color(225, 120, 30);
            } else{ //연작
                bgTheme = new Color(242, 240, 255); fgTheme = new Color(110, 90, 200);
            }

            JLabel lblBadge = new JLabel(finalBadgeText, JLabel.CENTER){
                @Override
                protected void paintComponent(Graphics g){
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    //컴포넌트 중앙 정렬 기준 패딩 내부 박스 크기 산출
                    int w = getWidth();
                    int h = getHeight();
                    int bWidth = w-16;  //좌우 여백 확보
                    int bHeight = 24;   //배지 표준 세로 높이
                    int bx = (w-bWidth)/2;
                    int by = (h-bHeight)/2;

                    //둥근 사각형 배경 드로잉
                    g2.setColor(bgTheme);
                    g2.fillRoundRect(bx, by, bWidth, bHeight, 10, 10);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            lblBadge.setFont(new Font("맑은 고딕", Font.BOLD, 11));
            lblBadge.setForeground(fgTheme);
            lblBadge.setPreferredSize(new Dimension(75, 54));

            rGbc.gridx = 0; rGbc.weightx = 0.0; row.add(lblBadge, rGbc);

            // 2. 제목/해시태그 소제목 복합 2줄 요약 패널 정밀 배치
            JPanel titleGridCell = new JPanel();
            titleGridCell.setLayout(new BoxLayout(titleGridCell, BoxLayout.Y_AXIS));
            titleGridCell.setOpaque(false);
            titleGridCell.setBorder(BorderFactory.createEmptyBorder(8, 5, 6, 5));

            //제목 레이블을 웹 링크 스타일로 확장 연동 (제목 클릭하면 뷰어 열림)
            JLabel lblTitle = new JLabel(sn.getTitle(), JLabel.LEFT);
            lblTitle.setFont(UiStyle.FONT_BOLD_13);
            lblTitle.setForeground(new Color(40, 45, 50));
            lblTitle.setCursor(new Cursor(Cursor.HAND_CURSOR));

            //제목 마우스 리스너 이식
            lblTitle.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (searchField != null && searchField.hasFocus()) {
                        categoryAnchorPanel.requestFocusInWindow();
                    }
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        boolean isFavItem = sn.getFolderPath().contains(":\\");

                        // 1. 타겟 파일/폴더 경로 설정
                        File targetPath;
                        if (isFavItem) {
                            targetPath = new File(sn.getFolderPath());
                        } else if (sn.getSnippetType().equals("연작")) {
                            targetPath = new File("C:\\novel\\novels\\short_stories\\" + sn.getParentWork() + "\\연작\\" + sn.getFolderPath());
                        } else {
                            targetPath = new File("C:\\novel\\novels\\short_stories\\" + sn.getParentWork() + "\\" + sn.getSnippetType() + "\\" + sn.getFolderPath());
                        }

                        System.out.println("--- 경로 디버깅 ---");
                        System.out.println("타겟 경로 존재 여부: " + targetPath.exists());
                        System.out.println("타겟 경로 문자열: " + targetPath.getAbsolutePath());
                        System.out.println("연작 폴더 여부: " + targetPath.isDirectory());

                        // 2. 연작 처리 분기
                        if (sn.getSnippetType().equals("연작")) {
                            File seriesDir = isFavItem ? new File(sn.getFolderPath()).getParentFile() : targetPath;

                            if (seriesDir != null && seriesDir.exists()) {
                                File[] subFiles = seriesDir.listFiles((d, n) -> n.toLowerCase().endsWith(".txt") && !n.toLowerCase().equals("bookmark.txt"));
                                java.util.List<File> sList = new java.util.ArrayList<>();
                                if (subFiles != null) {
                                    java.util.Collections.addAll(sList, subFiles);
                                    java.util.Collections.sort(sList, (f1, f2) -> f1.getName().compareTo(f2.getName()));
                                }

                                if (!sList.isEmpty()) {
                                    ShortStoryViewerPage viewer = new ShortStoryViewerPage();
                                    viewer.setNovelContext(sn, ShortStoryPanel.this);

                                    // [핵심 수정] 타겟이 파일이면 그대로 열고, 폴더라면 목록의 첫 파일을 엽니다.
                                    File fileToOpen = (targetPath.isFile()) ? targetPath : sList.get(0);

                                    viewer.openViewer(fileToOpen);
                                    viewer.setSeriesOrderData(sList, fileToOpen);
                                    viewer.setMetaInformation(sn.getAuthor(), sn.getDescription(), sn.getPlatform(), sn.getWordCount());
                                } else {
                                    JOptionPane.showMessageDialog(mainFrame, "해당 연작 폴더 내에 읽을 수 있는 .txt 원고 파일이 없습니다.", "안내", JOptionPane.WARNING_MESSAGE);
                                }
                            }
                        }
                        // 3. 단편/썰 처리 분기
                        else {
                            if (targetPath.exists()) {
                                ShortStoryViewerPage viewer = new ShortStoryViewerPage();
                                viewer.setNovelContext(sn, ShortStoryPanel.this);
                                viewer.openViewer(targetPath);
                                viewer.setSeriesOrderData(null, targetPath);
                                viewer.setMetaInformation(sn.getAuthor(), sn.getDescription(), sn.getPlatform(), sn.getWordCount());
                            } else {
                                JOptionPane.showMessageDialog(mainFrame, "파일 경로를 찾을 수 없습니다: " + targetPath, "오류", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    // 마우스를 올렸을 때 밑줄 효과 및 색상 반전 (웹 링크 스타일)
                    lblTitle.setText("<html><u>" + sn.getTitle() + "</u></html>");
                    lblTitle.setForeground(new Color(0, 120, 180));
                }
                @Override
                public void mouseExited(MouseEvent e){
                    //마우스가 벗어낫을 때 순정 상태로 복원
                    lblTitle.setText(sn.getTitle());
                    lblTitle.setForeground(new Color(40, 45, 50));
                }
            });

            //실시간 해시태그 주석 마킹 추출 가이드 정렬
            //키워드가 존재한다면 샵(#) 기호 연동 마킹 가독성 추가
            JLabel lblSubKeys = new JLabel("");
            if(sn.getKeywords() != null && !sn.getKeywords().trim().isEmpty()){
                //쉼표 앞뒤에 공백이 존재해도 깔끔하게 분리하도록 정규식 적용
                String[] split = sn.getKeywords().split("\\s*,\\s*");
                StringBuilder sb = new StringBuilder(" ");
                for(String k : split) {
                    if(!k.trim().isEmpty()) sb.append("#").append(k.trim()).append(" ");
                }
                lblSubKeys.setText(sb.toString().trim());
            } else{
                lblSubKeys.setText("등록된 키워드 태그가 없습니다.");
            }
            lblSubKeys.setFont(UiStyle.FONT_PLAIN_11);
            lblSubKeys.setForeground(new Color(0, 140, 180));

            titleGridCell.add(lblTitle);
            titleGridCell.add(Box.createVerticalStrut(3));
            titleGridCell.add(lblSubKeys);

            rGbc.gridx = 1; rGbc.weightx = 1.0; row.add(titleGridCell, rGbc);

            // 3. 분량(글자 수) 정렬 칸(85px)
            JLabel lblRowWords = new JLabel(String.format("%,d자", sn.getWordCount()), JLabel.CENTER);
            lblRowWords.setFont(UiStyle.FONT_PLAIN_12);
            lblRowWords.setForeground(Color.LIGHT_GRAY);
            lblRowWords.setPreferredSize(new Dimension(85, 54));
            rGbc.gridx = 2; rGbc.weightx = 0.0; row.add(lblRowWords, rGbc);

            // 4. 작가 닉네임(110px)
            JLabel lblRowAuth = new JLabel(sn.getAuthor(), JLabel.CENTER);
            lblRowAuth.setFont(UiStyle.FONT_PLAIN_12);
            lblRowAuth.setForeground(new Color(70, 75, 85));
            lblRowAuth.setPreferredSize(new Dimension(110, 54));
            rGbc.gridx = 3; rGbc.weightx = 0.0; row.add(lblRowAuth, rGbc);

            // 5. 플랫폼 출처 칸(100px 고정)
            JLabel lblPlat = new JLabel(sn.getPlatform(), JLabel.CENTER);
            lblPlat.setFont(UiStyle.FONT_PLAIN_12);
            lblPlat.setForeground(new Color(110, 115, 125));
            lblPlat.setPreferredSize(new Dimension(95, 54));
            rGbc.gridx = 4; rGbc.weightx = 0.0; row.add(lblPlat, rGbc);

            // 6. 우측 미니 옵션 단추 공란 칸(50px 고정)
            JLabel lblOpt = new JLabel("︙", JLabel.CENTER);
            lblOpt.setFont(new Font("맑은 고딕", Font.BOLD, 15));
            lblOpt.setForeground(new Color(170, 175, 185));
            lblOpt.setPreferredSize(new Dimension(35, 54));
            lblOpt.setCursor(new Cursor(Cursor.HAND_CURSOR));

            //︙ 기호 클릭 시 메타데이터 수정 다이얼로그 팝업 엔진 연동 리스너
            lblOpt.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    //row 전체에 걸려있는 뷰어 오픈 이벤트를 일시 정지 차단
                    e.consume();

                    JTextField authField = new JTextField(sn.getAuthor());
                    JTextField subTitleField = new JTextField(sn.getDescription() != null ? sn.getDescription() : "");
                    JTextField keyField = new JTextField(sn.getKeywords());

                    JComboBox<String> platCombo = new JComboBox<>(new String[]{"포스타입", "트위터", "기타"});
                    platCombo.setSelectedItem(sn.getPlatform());

                    Object[] messageForm = {
                            "작가 닉네임 수정:", authField,
                            "작품 소제목 수정:", subTitleField,
                            "태그/키워드 수정 (쉼표구분):", keyField,
                            "출신 플랫폼", platCombo
                    };

                    int option = JOptionPane.showConfirmDialog(mainFrame, messageForm,
                            "단편 메타데이터 속성 보정", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                    if(option == JOptionPane.OK_OPTION){
                        sn.setAuthor(authField.getText().trim().isEmpty() ? "작자미상" : authField.getText().trim());
                        sn.setDescription(subTitleField.getText().trim());
                        sn.setKeywords(keyField.getText().trim());
                        sn.setPlatform((String) platCombo.getSelectedItem());

                        //세이브 버퍼 배열 업데이트 및 디스크 영구 영동 저장 트리거 가동
                        updateSnippetMetadataInList(sn);
                        saveShortsData();
                        renderRightSnippetList();   //수정 완료 즉시 화면 리프레시 리렌더링
                    }
                }
            });
            rGbc.gridx = 5; rGbc.weightx = 0.0; row.add(lblOpt, rGbc);

            JPanel rowWrapper = new JPanel(new BorderLayout());
            rowWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
            rowWrapper.add(row, BorderLayout.CENTER);

            snippetListContainer.add(rowWrapper);
        }

        snippetListContainer.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if(searchField != null && searchField.hasFocus()){
                    categoryAnchorPanel.requestFocusInWindow();
                }
            }
        });

        //5단 묶음 동적 페이징 컨트롤 블록 알고리즘 이식 구역
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 12));
        paginationPanel.setOpaque(false);

        //5개 단위의 페이지 묶음 블록 인덱스 연산
        int currentBlock = (currentPage - 1)/5;
        int startPage = currentBlock * 5 + 1;
        int endPage = Math.min(startPage + 4, totalPages);

        // [<] 이전 블록 버튼 : 누르면 무조건 이전 묶음의 첫 페이지로 대형 도약
        JButton btnPrevBlock = new JButton("<");
        btnPrevBlock.setFont(UiStyle.FONT_BOLD_12);
        btnPrevBlock.setForeground(startPage > 1 ? UiStyle.COLOR_ACCENT : Color.LIGHT_GRAY);
        btnPrevBlock.setContentAreaFilled(false);
        btnPrevBlock.setBorderPainted(false);
        btnPrevBlock.setFocusPainted(false);
        if(startPage > 1){
            btnPrevBlock.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnPrevBlock.addActionListener(e -> {
                currentPage = startPage - 5;    //이전 묶음 첫번째 페이지 조준 점프
                renderRightSnippetList();
            });
        }
        paginationPanel.add(btnPrevBlock);

        //중앙 숫자 페이지 버튼 시퀸스 드로잉
        for(int p = startPage; p <= endPage; p++){
            final int pageIdx = p;
            JButton btnPage = new JButton(String.valueOf(p)){
                @Override
                protected void paintComponent(Graphics g){
                    if(currentPage == pageIdx){
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        g2.setColor(new Color(0, 150, 150));
                        g2.fillOval(4, 4, getWidth()-8, getHeight()-8);
                        g2.dispose();
                    }
                    super.paintComponent(g);
                }
            };
            btnPage.setFont(new Font("맑은 고딕", currentPage == p ? Font.BOLD : Font.PLAIN, 12));
            btnPage.setForeground(currentPage == p ? Color.WHITE : new Color(90, 95, 105));
            btnPage.setPreferredSize(new Dimension(32, 32));
            btnPage.setContentAreaFilled(false);
            btnPage.setBorderPainted(false);
            btnPage.setFocusPainted(false);
            btnPage.setCursor(new Cursor(Cursor.HAND_CURSOR));

            btnPage.setMargin(new Insets(0, 0, 0, 0));

            btnPage.addActionListener(e -> {
                currentPage = pageIdx;
                renderRightSnippetList();
            });
            paginationPanel.add(btnPage);
        }

        //[>] 다음 블록 버튼 : 누르면 무조건 다음 묶음의 첫 페이지(6, 11, 16, ...)으로 점프
        JButton btnNextBlock = new JButton(">");
        btnNextBlock.setFont(UiStyle.FONT_BOLD_12);
        btnNextBlock.setForeground(endPage < totalPages ? UiStyle.COLOR_ACCENT : Color.LIGHT_GRAY);
        btnNextBlock.setContentAreaFilled(false);
        btnNextBlock.setBorderPainted(false);
        btnNextBlock.setFocusPainted(false);
        if(endPage < totalPages){
            btnNextBlock.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnNextBlock.addActionListener(e -> {
                currentPage = startPage + 5;
                renderRightSnippetList();
            });
        }
        paginationPanel.add(btnNextBlock);

        snippetListContainer.add(paginationPanel);

        snippetListContainer.revalidate();
        snippetListContainer.repaint();
    }

    private Snippet findSaveMetadata(String work, String type, String fileName){
        for(Snippet sn : snippetList){
            //원작명, 폴더유형, 텍스트 파일 실제 이름 3가지 매칭 검증을 통해 유저 세이브 매핑 반환
            if(sn.getParentWork().equals(work) &&
                    sn.getSnippetType().equals(type) &&
                    sn.getFolderPath() != null && sn.getFolderPath().equals(fileName)){
                return sn;
            }
        }
        return null;
    }

    private void updateSnippetMetadataInList(Snippet target){
        for(int i=0; i<snippetList.size(); i++){
            Snippet sn = snippetList.get(i);
            //원작명, 분류유형, 파일명이 일치하는 대상을 정밀 타킹
            if(sn.getParentWork().equals(target.getParentWork()) &&
            sn.getSnippetType().equals(target.getSnippetType()) &&
            sn.getFolderPath().equals(target.getFolderPath())){

                //기존 데이터에 읽은 날짜가 기록되어 있다면 유실되지 않도록 가드 보정
                if(target.getLastReadDate() == null || target.getLastReadDate().equals("기록 없음")){
                    if(sn.getLastReadDate() != null && !sn.getLastReadDate().equals("기록 없음")){
                        target.setLastReadDate(sn.getLastReadDate());
                    }
                }
                snippetList.set(i, target);
                return;
            }
        }
        snippetList.add(target);
    }

    //shorts_data.txt 데이터 입출력 및 영구 저장 백엔드 시스템
    //메모리에 적재된 snippetList 스냅샷을 파일에 동기화 백업
    public void saveShortsData(){
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(SHORTS_DATA_FILE))){
            for(Snippet sn : snippetList){
                String line = sn.getTitle() + "|" +
                        sn.getAuthor() + "|" +
                        sn.getPlatform() + "|" +
                        sn.getFolderPath() + "|" +      //파일 실물 네임 태그 저장용으로 우회 활용
                        sn.getParentWork() + "|" +
                        sn.getSnippetType() + "|" +
                        sn.getWordCount() + "|" +
                        sn.getKeywords() + "|" +
                        sn.getDescription() + "|" +
                        sn.getLastReadDate() + "|" +
                        sn.isFavorite() + "|" +
                        sn.getCreatedDate();
                bw.write(line);
                bw.newLine();
            }
        } catch(IOException e){
            System.out.println("단편 저장 에러: " + e.getMessage());
        }
    }

    //shorts_data.txt 보정 메타데이터 복원 복구 로더
    private void loadShortsData(){
        File file = new File(SHORTS_DATA_FILE);
        snippetList.clear();

        if(!file.exists()){
            //최초 기동 시 빈 파일 생성 자동 마감 가드
            saveShortsData();
            refreshShortStoryLibrary();
            return;
        }
        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String line;
            while((line = br.readLine()) != null){
                String[] data = line.split("\\|", -1);
                if(data.length >= 11){
                    Snippet sn = new Snippet(
                            data[0], data[1], data[2], data[3], data[4],
                            data[5], Integer.parseInt(data[6]), data[7], data[8],
                            data[9], Boolean.parseBoolean(data[10])
                    );
                    if(data.length >= 12) sn.setCreatedDate(data[11]);
                    snippetList.add(sn);
                }
            }
        } catch(Exception e){
            System.out.println("단편 로드 에러: " + e.getMessage());
        }
        //로드 직후 렌더링 엔진 결합 트리거 가동
        refreshShortStoryLibrary();

        //복원 즉시 우측 와이드 배너 및 데이터 소설 리스트 강제 렌더링 지시
        renderRightSnippetList();
    }

    //원작 폴더 링크 데이터 디스크 저장 백엔드
    private void saveWorkLinksData(){
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(WORK_LINKS_FILE))){
            for(String work : workFolderMap.keySet()){
                bw.write(work + "|" + workFolderMap.get(work));
                bw.newLine();
            }
        } catch(IOException e){
            System.out.println("원작 링크 저장 에러: " + e.getMessage());
        }
    }

    //원작 폴더 링크 데이터 부팅 로더
    private void loadWorkLinksData(){
        workFolderMap.clear();
        File file = new File(WORK_LINKS_FILE);
        if(!file.exists()) return;
        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String line;
            while((line = br.readLine()) != null){
                String[] data = line.split("\\|", -1);
                if(data.length >= 2) workFolderMap.put(data[0], data[1]);
            }
        } catch(Exception e){
            System.out.println("원작 링크 로드 에러: " + e.getMessage());
        }
    }

    //하위 폴더 및 잔존 가드 파일까지 삭제하는 디스크 포맷 재귀 엔진 메서드
    private void deleteFolderRecursively(File folder){
        File[] files = folder.listFiles();
        if(files != null){
            for(File f : files){
                if(f.isDirectory()) deleteFolderRecursively(f);
                else f.delete();
            }
        }
        folder.delete();
    }

    // 1. 플랫폼 데이터 파일 불러오기
    private void loadPlatformsData(){
        platformItems.clear();
        File file = new File(PLATFORMS_FILE);
        if(file.exists()){
            try(BufferedReader br = new BufferedReader(new FileReader(file))){
                String line;
                while((line = br.readLine()) != null){
                    if(!line.trim().isEmpty()) platformItems.add(line.trim());
                }
            } catch(Exception e){
                System.out.println("플랫폼 로드 에러: " + e.getMessage());
            }
        }
        // 파일이 없거나 비어있을 경우 기본 세팅
        if(platformItems.isEmpty()){
            platformItems.add("포스타입");
            platformItems.add("트위터");
        }
    }

    // 2. 플랫폼 데이터 파일 저장하기
    private void savePlatformsData(){
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(PLATFORMS_FILE))){
            for(String p : platformItems){
                bw.write(p);
                bw.newLine();
            }
        } catch(Exception e){
            System.out.println("플랫폼 저장 에러: " + e.getMessage());
        }
    }


    // 4. 플랫폼 관리 모달 창 UI
    private void showPlatformManagerDialog(){
        JDialog dialog = new JDialog(mainFrame, "플랫폼 관리", true);
        dialog.setSize(300, 300);
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.WHITE);

        // 상단: 새 플랫폼 추가 영역
        JPanel topPanel = new JPanel(new BorderLayout(8, 0));
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField tfNewPlatform = new JTextField();
        tfNewPlatform.setFont(UiStyle.FONT_PLAIN_12);
        tfNewPlatform.setPreferredSize(new Dimension(0, 30));

        JButton btnAdd = new JButton("추가");
        btnAdd.setFont(UiStyle.FONT_BOLD_12);
        btnAdd.setForeground(UiStyle.COLOR_ACCENT);
        btnAdd.setContentAreaFilled(false);
        btnAdd.setBorder(BorderFactory.createLineBorder(UiStyle.COLOR_ACCENT, 1, true));
        btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdd.setPreferredSize(new Dimension(50, 30));

        topPanel.add(tfNewPlatform, BorderLayout.CENTER);
        topPanel.add(btnAdd, BorderLayout.EAST);

        // 중앙: 플랫폼 리스트 스크롤 영역
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 235, 240)));
        scrollPane.getViewport().setBackground(Color.WHITE);

        // 리스트를 다시 그리는 지역 함수
        Runnable refreshList = new Runnable() {
            @Override
            public void run() {
                listPanel.removeAll();
                for(String p : platformItems){
                    JPanel row = new JPanel(new BorderLayout());
                    row.setBackground(Color.WHITE);
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                    row.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(245, 246, 248)),
                            BorderFactory.createEmptyBorder(0, 15, 0, 10)
                    ));

                    JLabel lblName = new JLabel(p);
                    lblName.setFont(UiStyle.FONT_PLAIN_13);
                    lblName.setForeground(UiStyle.COLOR_LABEL_TEXT);

                    JButton btnDel = new JButton("삭제");
                    btnDel.setFont(new Font("맑은 고딕", Font.BOLD, 11));
                    btnDel.setForeground(new Color(220, 80, 80));
                    btnDel.setContentAreaFilled(false);
                    btnDel.setBorderPainted(false);
                    btnDel.setFocusPainted(false);
                    btnDel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    btnDel.setMargin(new Insets(0, 0, 0, 0));

                    btnDel.addActionListener(e -> {
                        int confirm = JOptionPane.showConfirmDialog(dialog, "'" + p + "' 플랫폼을 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
                        if(confirm == JOptionPane.YES_OPTION){
                            platformItems.remove(p);
                            savePlatformsData();

                            // 현재 선택된 플랫폼이 삭제되었다면 '전체 플랫폼'으로 뷰 초기화
                            if (selectedPlatform.equals(p)) {
                                selectedPlatform = "전체 플랫폼";
                                btnPlatformSelect.setText("전체 플랫폼 ▼");
                                renderRightSnippetList();
                            }

                            this.run(); //UI 새로고침
                        }
                    });

                    row.add(lblName, BorderLayout.CENTER);
                    row.add(btnDel, BorderLayout.EAST);
                    listPanel.add(row);
                }
                listPanel.revalidate();
                listPanel.repaint();
            }
        };

        // 추가 버튼 클릭 이벤트
        btnAdd.addActionListener(e -> {
            String newP = tfNewPlatform.getText().trim();
            if(!newP.isEmpty()){
                if(platformItems.contains(newP)){
                    JOptionPane.showMessageDialog(dialog, "이미 존재하는 플랫폼입니다.");
                } else{
                    platformItems.add(newP);
                    savePlatformsData();
                    tfNewPlatform.setText("");
                    refreshList.run();
                }
            }
        });

        // 엔터키 지원
        tfNewPlatform.addActionListener(e -> btnAdd.doClick());

        refreshList.run();  // 초기 렌더링

        dialog.add(topPanel, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private JPanel createGuideTipContainer() {
        JPanel tipContainer = new JPanel();
        tipContainer.setLayout(new BoxLayout(tipContainer, BoxLayout.Y_AXIS));
        tipContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        tipContainer.setBackground(new Color(250, 252, 253));

        String[] guides = {
                "※ 원작 폴더 구조 규칙",
                "",
                "1. 단편/썰: 폴더 안에 .txt 파일 배치",
                "   예) ...\\short_stories\\[원작명]\\단편\\제목.txt",
                "",
                "2. 연작: 폴더 안에 '작품 폴더'를 생성하고",
                "   그 내부에 .txt 파일들을 배치하세요.",
                "   예) ...\\short_stories\\[원작명]\\연작\\[작품제목]\\1화.txt",
                "",
                "※ 이 규칙을 따라야 목록에 정상 노출됩니다."
        };

        for (String text : guides) {
            JLabel label = new JLabel(text);
            label.setFont(new Font("맑은 고딕", text.startsWith("※") ? Font.BOLD : Font.PLAIN, 12));
            tipContainer.add(label);
        }
        return tipContainer;
    }
}
