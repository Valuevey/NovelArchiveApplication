import javax.crypto.CipherInputStream;
import javax.management.Descriptor;
import javax.management.IntrospectionException;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class AddNovelDialog extends JDialog {
    private JTextField tfTitle;
    private JTextField tfAuthor;
    private JTextField tfGenre;
    private JTextField tfKeywords;
    private JTextArea taDescription;
    private JTextField tfFolderPath;
    private JTextField tfCoverPath;

    //다중 플랫폼 선택용 체크박스들
    private JCheckBox cbSeries, cbKakao, cbJoara, cbMunpia;

    //완결 여부를 선택할 수 있는 체크박스 전역 변수 신설
    private JCheckBox cbCompleted;

    //결과 데이터를 보관함 페이지로 넘겨주기 위한 변수
    private Novel resultNovel = null;

    //현재 모드가 수정 모드인지 판별하고 기존 값을 유지하기 위한 참조 변수
    private Novel existingNovel = null;

    public AddNovelDialog(JFrame parent){
        this(parent, null);   //2번으로 토스 연계
        //부모 창을 제어할 수 없게 잠그는 Modal모드로 설정
    }

    //[소설 정보 수정] 및 대통합 처리를 위한 2차 생성자
    public AddNovelDialog(Window parent, Novel novelToEdit){
        //JDialog 사양에 맞춰 부모 창 타입을 Modal 모드로 셋업
        super(parent, novelToEdit == null ? "새 소설 등록" : "소설 정보 수정", ModalityType.APPLICATION_MODAL);
        this.existingNovel = novelToEdit;

        setSize(610, 700);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        Color themeCyan = new Color(0, 140, 140);
        Color borderGray = new Color(225, 228, 232);

        //1. 메인 컨테이너 패널(BoxLayout을 이용해 위에서 아래롤 컴포넌트를 쌓음)
        JPanel contentGridPanel = new JPanel();
        contentGridPanel.setBackground(Color.WHITE);
        contentGridPanel.setLayout(new BoxLayout(contentGridPanel, BoxLayout.Y_AXIS));
        contentGridPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 15,25));

        // 소설 제목의 글자와 상자가 가로로 배치되도록 패널 개설
        JPanel rowTitlePanel = new JPanel(new BorderLayout(10, 0));
        rowTitlePanel.setBackground(Color.WHITE);
        rowTitlePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowTitlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        //소설 제목
        JLabel lblTitle = new JLabel("소설 제목 *");
        lblTitle.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        lblTitle.setForeground(new Color(50, 55, 60));
        lblTitle.setPreferredSize(new Dimension(130, 36));


        JTextField tfTitle = new JTextField(){
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(themeCyan); // 강조 필드는 청록색 프레임으로 정밀 인쇄
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tfTitle.setOpaque(false);
        tfTitle.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        tfTitle.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        rowTitlePanel.add(lblTitle, BorderLayout.WEST);
        rowTitlePanel.add(tfTitle, BorderLayout.CENTER);
        contentGridPanel.add(rowTitlePanel);
        contentGridPanel.add(Box.createVerticalStrut(14));

        //작가 이름 영역도 글과 상자가 가로로 배치되도록 패널 생성
        JPanel rowAuthorPanel = new JPanel(new BorderLayout(10, 0));
        rowAuthorPanel.setBackground(Color.WHITE);
        rowAuthorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowAuthorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        //작가 이름
        JLabel lblAuthor = new JLabel("작가 이름");
        lblAuthor.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        lblAuthor.setForeground(new Color(50, 55, 60));
        lblAuthor.setPreferredSize(new Dimension(130, 36));

        JTextField tfAuthor = new JTextField() {
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
        tfAuthor.setOpaque(false);
        tfAuthor.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        tfAuthor.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        rowAuthorPanel.add(lblAuthor, BorderLayout.WEST);
        rowAuthorPanel.add(tfAuthor, BorderLayout.CENTER);
        contentGridPanel.add(rowAuthorPanel);
        contentGridPanel.add(Box.createVerticalStrut(14));

        //장르 칸 가로 배치를 위한 패널
        JPanel rowGenrePanel = new JPanel(new BorderLayout(10, 0));
        rowGenrePanel.setBackground(Color.WHITE);
        rowGenrePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowGenrePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        // 장르 입력 구역
        JLabel lblGenre = new JLabel("장르 (ex. 판타지)");
        lblGenre.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        lblGenre.setForeground(new Color(50, 55, 60));
        lblGenre.setPreferredSize(new Dimension(130, 36));

        JTextField tfGenre = new JTextField() {
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
        tfGenre.setOpaque(false);
        tfGenre.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        tfGenre.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        rowGenrePanel.add(lblGenre, BorderLayout.WEST);
        rowGenrePanel.add(tfGenre, BorderLayout.CENTER);
        contentGridPanel.add(rowGenrePanel);
        contentGridPanel.add(Box.createVerticalStrut(14));


        //출신 플랫폼
        JPanel rowPlatformMasterPanel = new JPanel(new BorderLayout(10, 0));
        rowPlatformMasterPanel.setBackground(Color.WHITE);
        rowPlatformMasterPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        //수축 및 확장 시 높이 변화를 보관하기 위한 상태 관리용 가변 바운더리 객체 선언
        int maxCols = 3;         //1줄당 최대 3개 배치
        java.util.List<String> pNameList = AppSettings.getInstance().getCustomPlatforms();
        int totalPlatforms = pNameList.size();

        //6개 이하라면 2(72px)줄 고정, 7개 이상이어도 최초 수축 상태는 2줄 높이로 제한
        int fixedPlatformHeight = 72;
        rowPlatformMasterPanel.setMinimumSize(new Dimension(10, fixedPlatformHeight));
        rowPlatformMasterPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, fixedPlatformHeight));
        rowPlatformMasterPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, fixedPlatformHeight));

        JLabel lblPlatformGroup = new JLabel("<html><body>출신 플랫폼<br><font size='3' color='#888888'>(중복 가능)</font></body></html>");
        lblPlatformGroup.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        lblPlatformGroup.setForeground(new Color(50, 55, 60));
        lblPlatformGroup.setPreferredSize(new Dimension(130, 45));

        java.util.List<JCheckBox> cryptoCheckBoxes = new java.util.ArrayList<>();

        //플랫폼 격자 바구니를 내부 익명 클래스 영역 밖으로 격리
        JPanel platformGridWrapper = new JPanel(new GridLayout(2, maxCols, 8, 8));
        platformGridWrapper.setBackground(Color.WHITE);

        //7번째 이후 플랫폼 항목을 하단 콤보박스처럼 독립적으로 띄워 배포할 인프라 개설
        JPopupMenu platformDropdownPopup = new JPopupMenu();
        platformDropdownPopup.setBackground(Color.WHITE);
        platformDropdownPopup.setBorder(BorderFactory.createLineBorder(borderGray, 1));

        //체크박스를 3열로 담을 패널
        int extraCount = totalPlatforms - 6;
        int popupRows = (int) Math.ceil((double) (extraCount > 0 ? extraCount : 0) / maxCols);
        if (popupRows < 1) popupRows = 1;
        JPanel popupGridContainer = new JPanel(new GridLayout(popupRows, maxCols, 8, 8));
        popupGridContainer.setBackground(Color.WHITE);
        popupGridContainer.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8)); // 팝업 테두리 내부 쿠션 마진 지정


        for (int i=0; i<totalPlatforms; i++) {
            String platformName = pNameList.get(i);
            JCheckBox chk = new JCheckBox(platformName) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isSelected() ? new Color(240, 250, 250) : Color.WHITE);
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    g2.setColor(isSelected() ? themeCyan : borderGray);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            chk.setOpaque(false);
            chk.setFocusPainted(false);
            chk.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            chk.setForeground(new Color(60, 65, 75));
            chk.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            chk.setCursor(new Cursor(Cursor.HAND_CURSOR));
            chk.addActionListener(e -> rowPlatformMasterPanel.repaint());

            // 정보 수정 모드일 때 기존 체크 상태 자동 복원 매핑
            if (existingNovel != null && existingNovel.getPlatform().contains(platformName)) {
                chk.setSelected(true);
            }

            cryptoCheckBoxes.add(chk);

            //6개 이하 데이터는 상단 기본 기조 서식에 안착, 7개째 부터는 메인 화면이 아닌 팝업 드롭다운 리스트로 분리
            if(i<6){
                platformGridWrapper.add(chk);
            } else{
                //팝업 내부 체크박스 규격 최적화 설정
                chk.setPreferredSize(new Dimension(115, 32));
                popupGridContainer.add(chk);
            }
        }
        //기본 상단 2줄 화면에서 빈 칸이 생기는 경우 레이아웃 깨지는거 방지용으로 더미 넣음
        int visibleInBase = Math.min(totalPlatforms, 6);
        int dummyCount = (maxCols - (visibleInBase % maxCols)) % maxCols;
        for (int i = 0; i < dummyCount; i++) {
            JPanel dummy = new JPanel();
            dummy.setOpaque(false);
            platformGridWrapper.add(dummy);
        }

        if (totalPlatforms > 6) {
            int extraComponents = popupGridContainer.getComponentCount();
            int popupDummyCount = (maxCols - (extraComponents % maxCols)) % maxCols;
            for (int i = 0; i < popupDummyCount; i++) {
                JPanel dummy = new JPanel();
                dummy.setOpaque(false);
                popupGridContainer.add(dummy);
            }
            platformDropdownPopup.add(popupGridContainer);
        }

        rowPlatformMasterPanel.add(lblPlatformGroup, BorderLayout.WEST);
        rowPlatformMasterPanel.add(platformGridWrapper, BorderLayout.CENTER);

        // 플랫폼이 7개 이상일 때만 우측에 배동될 커스텀 V자 화살표 버튼
        if(totalPlatforms > 6){
            JButton btnToggleExtend = new JButton(){
                private boolean isExpanded = false; //화살표 방향 드로잉용

                @Override
                protected void paintComponent(Graphics g){
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.setColor(new Color(120, 130, 140));

                    int cx = getWidth()/2;
                    int cy = getHeight()/2;

                    //플래그 상태에 따라 화살표 꺽쇠 문양 방향을 실시간 반전 드로잉
                    if(!isExpanded){
                        g2.drawLine(cx - 5, cy - 2, cx, cy + 3);
                        g2.drawLine(cx, cy + 3, cx + 5, cy - 2);
                    } else{
                        g2.drawLine(cx - 5, cy + 3, cx, cy - 2);
                        g2.drawLine(cx, cy - 2, cx + 5, cy + 3);
                    }
                    g2.dispose();
                }
            };
            btnToggleExtend.setOpaque(false);
            btnToggleExtend.setContentAreaFilled(false);
            btnToggleExtend.setBorderPainted(false);
            btnToggleExtend.setFocusPainted(false);
            btnToggleExtend.setPreferredSize(new Dimension(25, 72));    //2줄 높이
            btnToggleExtend.setCursor(new Cursor(Cursor.HAND_CURSOR));

            // V버튼 클릭 시 레이아웃 제한 높이를 실시간 재연산하여 화면을 늘리고 줄이는 리스너를 결합
            btnToggleExtend.addActionListener(e ->{
                try {
                    java.lang.reflect.Field field = btnToggleExtend.getClass().getDeclaredField("isExpanded");
                    field.setAccessible(true);
                    boolean currentVal = field.getBoolean(btnToggleExtend);

                    if (!currentVal) {
                        // 팝업창을 플랫폼 상자 바로 아랫단 축에 맞춰 레이어링 스위칭 출격합니다.
                        platformDropdownPopup.show(platformGridWrapper, 0, platformGridWrapper.getHeight() + 4);
                        field.set(btnToggleExtend, true);
                    } else {
                        platformDropdownPopup.setVisible(false);
                        field.set(btnToggleExtend, false);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                btnToggleExtend.repaint();
            });

            platformDropdownPopup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}
                @Override
                public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            java.lang.reflect.Field field = btnToggleExtend.getClass().getDeclaredField("isExpanded");
                            field.setAccessible(true);

                            Point mousePoint = MouseInfo.getPointerInfo().getLocation();
                            SwingUtilities.convertPointFromScreen(mousePoint, btnToggleExtend);

                            if (btnToggleExtend.contains(mousePoint)) {
                                field.set(btnToggleExtend, true);
                            } else {
                                field.set(btnToggleExtend, false);
                            }
                            btnToggleExtend.repaint();
                        } catch (Exception ex) { ex.printStackTrace(); }
                    });
                }
                @Override
                public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
            });

            rowPlatformMasterPanel.add(btnToggleExtend, BorderLayout.EAST);
        }
        contentGridPanel.add(rowPlatformMasterPanel);
        contentGridPanel.add(Box.createVerticalStrut(14));

        //완결 배지 구간
        JPanel rowCompletedMasterPanel = new JPanel(new BorderLayout(10, 0));
        rowCompletedMasterPanel.setBackground(Color.WHITE);
        rowCompletedMasterPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowCompletedMasterPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        Dimension skeletonSpacer = new Dimension(130, 36);
        Box.Filler leftSpacer = new Box.Filler(skeletonSpacer, skeletonSpacer, skeletonSpacer);

        //완결 배지 마킹 구역 붉은색 라운드 경고 배너 박스화
        cbCompleted = new JCheckBox("이 작품은 완결된 작품입니다. (완결 배지 마킹)");
        JPanel completedBannerCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cbCompleted.isSelected() ? new Color(255, 242, 242) : new Color(250, 252, 252));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(cbCompleted.isSelected() ? new Color(245, 190, 190) : borderGray);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        completedBannerCard.setOpaque(false);
        completedBannerCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        completedBannerCard.setPreferredSize(new Dimension(400, 36));

        cbCompleted.setOpaque(false);
        cbCompleted.setFocusPainted(false);
        cbCompleted.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        cbCompleted.setForeground(new Color(210, 50, 50));
        cbCompleted.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cbCompleted.addActionListener(e -> completedBannerCard.repaint());

        if (existingNovel != null && existingNovel.isCompleted()) { //
            cbCompleted.setSelected(true); //
        }

        completedBannerCard.add(cbCompleted);

        rowCompletedMasterPanel.add(leftSpacer, BorderLayout.WEST);
        rowCompletedMasterPanel.add(completedBannerCard, BorderLayout.CENTER);
        contentGridPanel.add(rowCompletedMasterPanel);
        contentGridPanel.add(Box.createVerticalStrut(14));

        // 키워드 입력 구역
        JPanel rowKeywordsPanel = new JPanel(new BorderLayout(10, 0));
        rowKeywordsPanel.setBackground(Color.WHITE);
        rowKeywordsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowKeywordsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel lblKeywords = new JLabel("키워드 (쉼표 구분)");
        lblKeywords.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        lblKeywords.setForeground(new Color(50, 55, 60));
        lblKeywords.setPreferredSize(new Dimension(130, 36));

        tfKeywords = new JTextField() {
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
        tfKeywords.setOpaque(false);
        tfKeywords.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        tfKeywords.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        rowKeywordsPanel.add(lblKeywords, BorderLayout.WEST);
        rowKeywordsPanel.add(tfKeywords, BorderLayout.CENTER);
        contentGridPanel.add(rowKeywordsPanel);
        contentGridPanel.add(Box.createVerticalStrut(14));

        //작품 소개글 대형 상자 라운드 프레임 마감
        JPanel rowDescPanel = new JPanel(new BorderLayout(10, 0));
        rowDescPanel.setBackground(Color.WHITE);
        rowDescPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowDescPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        JLabel lblDescription = new JLabel("작품 소개글");
        lblDescription.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        lblDescription.setForeground(new Color(50, 55, 60));
        lblDescription.setPreferredSize(new Dimension(130, 130));

        JTextArea taDescription = new JTextArea();
        taDescription.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        taDescription.setLineWrap(true);

        JScrollPane descScrollPane = new JScrollPane(taDescription) {
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
        descScrollPane.setOpaque(false);
        descScrollPane.getViewport().setOpaque(false);
        descScrollPane.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        rowDescPanel.add(lblDescription, BorderLayout.WEST);
        rowDescPanel.add(descScrollPane, BorderLayout.CENTER);
        contentGridPanel.add(rowDescPanel);
        contentGridPanel.add(Box.createVerticalStrut(14));

        //소설 텍스트 폴더 및 표지 파일라운드 행
        JPanel rowFolderPanel = new JPanel(new BorderLayout(10, 0));
        rowFolderPanel.setBackground(Color.WHITE);
        rowFolderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowFolderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel lblFolder = new JLabel("소설 텍스트 폴더 *");
        lblFolder.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        lblFolder.setForeground(new Color(50, 55, 60));
        lblFolder.setPreferredSize(new Dimension(130, 32));

        JPanel folderRowPanel = new JPanel(new BorderLayout(8, 0));
        folderRowPanel.setBackground(Color.WHITE);

        tfFolderPath = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(245, 246, 248));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(borderGray);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tfFolderPath.setOpaque(false);
        tfFolderPath.setEditable(false);
        tfFolderPath.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        tfFolderPath.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        JButton btnSelectFolder = new JButton("찾기") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(themeCyan);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnSelectFolder.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        btnSelectFolder.setForeground(themeCyan);
        btnSelectFolder.setContentAreaFilled(false);
        btnSelectFolder.setBorderPainted(false);
        btnSelectFolder.setPreferredSize(new Dimension(75, 32));
        btnSelectFolder.setFocusPainted(false);
        btnSelectFolder.setCursor(new Cursor(Cursor.HAND_CURSOR));

        folderRowPanel.add(tfFolderPath, BorderLayout.CENTER);
        folderRowPanel.add(btnSelectFolder, BorderLayout.EAST);

        rowFolderPanel.add(lblFolder, BorderLayout.WEST);
        rowFolderPanel.add(folderRowPanel, BorderLayout.CENTER);
        contentGridPanel.add(rowFolderPanel);
        contentGridPanel.add(Box.createVerticalStrut(14));

        //표지 이미지 파일 행 조립
        JPanel rowCoverPanel = new JPanel(new BorderLayout(10, 0));
        rowCoverPanel.setBackground(Color.WHITE);
        rowCoverPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowCoverPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel lblCover = new JLabel("표지 이미지 파일");
        lblCover.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        lblCover.setForeground(new Color(50, 55, 60));
        lblCover.setPreferredSize(new Dimension(130, 32));

        JPanel coverRowPanel = new JPanel(new BorderLayout(8, 0));
        coverRowPanel.setBackground(Color.WHITE);

        tfCoverPath = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(245, 246, 248));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(borderGray);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tfCoverPath.setOpaque(false);
        tfCoverPath.setEditable(false);
        tfCoverPath.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        tfCoverPath.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        JButton btnSelectCover = new JButton("찾기") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(themeCyan);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnSelectCover.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        btnSelectCover.setForeground(themeCyan);
        btnSelectCover.setContentAreaFilled(false);
        btnSelectCover.setBorderPainted(false);
        btnSelectCover.setPreferredSize(new Dimension(75, 32));
        btnSelectCover.setFocusPainted(false);
        btnSelectCover.setCursor(new Cursor(Cursor.HAND_CURSOR));

        coverRowPanel.add(tfCoverPath, BorderLayout.CENTER);
        coverRowPanel.add(btnSelectCover, BorderLayout.EAST);

        rowCoverPanel.add(lblCover, BorderLayout.WEST);
        rowCoverPanel.add(coverRowPanel, BorderLayout.CENTER);
        contentGridPanel.add(rowCoverPanel);

        //하단 조작 버튼
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(235, 238, 242)));
        bottomPanel.setBackground(new Color(245, 247, 249));
        bottomPanel.setPreferredSize(new Dimension(500, 75));

        JLabel lblDialogDecor = new JLabel();
        int dialogDecorSize = 150;
        String dialongDecorPath = "C:\\novel\\icon\\decor2.png";

        File dialogDecorFile = new File(dialongDecorPath);
        if(dialogDecorFile.exists()){
            try{
                ImageIcon dIcon = new ImageIcon(dialongDecorPath);
                Image scaledDImg = dIcon.getImage().getScaledInstance(dialogDecorSize, dialogDecorSize, Image.SCALE_SMOOTH);
                lblDialogDecor.setIcon(new ImageIcon(scaledDImg));
            } catch(Exception e){
                System.out.println("다이얼로그 데코 로드 실패: " + e.getMessage());
            }
        } else{
            lblDialogDecor.setPreferredSize(new Dimension(dialogDecorSize, dialogDecorSize));
        }
        lblDialogDecor.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        bottomPanel.add(lblDialogDecor, BorderLayout.WEST);     //하단 바 동쪽 벽면에 도킹

        //취소 및 등록 버튼을 세트로 묶어서 우측 정렬하기 위한 서브 패널
        JPanel rightButtonSuite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 18));
        rightButtonSuite.setOpaque(false);

        //취소 버튼 끝부분 둥글게
        JButton btnCancel = new JButton("취소") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(themeCyan); // 청록색 엣지 라인 드로잉
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnCancel.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        btnCancel.setForeground(themeCyan);
        btnCancel.setFocusPainted(false);
        btnCancel.setBorderPainted(false);
        btnCancel.setContentAreaFilled(false);
        btnCancel.setPreferredSize(new Dimension(95, 36));
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 등록 버튼 끝 둥글게
        JButton btnSave = new JButton(existingNovel == null ? "등록 완료" : "수정 완료") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(themeCyan); // 완전 청록색 채우기
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnSave.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setBorderPainted(false);
        btnSave.setContentAreaFilled(false);
        btnSave.setPreferredSize(new Dimension(135, 36));
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));

        rightButtonSuite.add(btnCancel);
        rightButtonSuite.add(btnSave);
        bottomPanel.add(rightButtonSuite, BorderLayout.EAST);   //하단 바 서쪽 벽면에 배치


        //수정 모드일 경우 각 컴포넌트 텍스트 필드에 기존 소설 메타데이터를 정밀 배치
        if(existingNovel != null){
            tfTitle.setText(existingNovel.getTitle());
            tfAuthor.setText(existingNovel.getAuthor());
            tfGenre.setText(existingNovel.getGenre());
            tfKeywords.setText(existingNovel.getKeywords());
            taDescription.setText(existingNovel.getDescription());
            tfFolderPath.setText(existingNovel.getFolderPath());
            tfCoverPath.setText(existingNovel.getCoverPath());
        }

        add(contentGridPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // 파일/폴더 선택 이벤트 연결
        btnSelectFolder.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser("C:\\novel\\novels");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);   // 폴더 선택만 가능하게 세팅
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                tfFolderPath.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        btnSelectCover.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser("C:\\novel\\covers");
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                tfCoverPath.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        btnCancel.addActionListener(e -> dispose());

        //등록 완료 클릭 시 데이터 유효성 검사 및 객체 생성
        btnSave.addActionListener(e -> {
            String title = tfTitle.getText().trim();
            String author = tfAuthor.getText().trim();
            String genre = tfGenre.getText().trim();
            String folderPath = tfFolderPath.getText();
            String coverPath = tfCoverPath.getText();
            String keywords = tfKeywords.getText().trim();
            String description = taDescription.getText().trim();

            //필수 입력 조건 체크(제목과 폴더는 필수)
            if(title.isEmpty() || folderPath.isEmpty()){
                JOptionPane.showMessageDialog(this, "소설 제목과 텍스트 폴더는 반드시 입력해야 합니다.", "경고", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ArrayList<String> pList = new ArrayList<>();
            for(JCheckBox chk : cryptoCheckBoxes){
                if(chk.isSelected()){
                    pList.add(chk.getText());
                }
            }

            String selectedPlatforms = String.join(", ", pList);
            if(selectedPlatforms.isEmpty()) selectedPlatforms = "기타";

            //수정 모드일 때는 기존의 최근열람일과 좋아요 하트 변수 상태를 보존 복사
            String lastRead = (existingNovel != null) ? existingNovel.getLastReadDate() : "기록 없음";
            boolean isFav = (existingNovel != null) ? existingNovel.isFavorite() : false;
            long favTime = (existingNovel != null) ? existingNovel.getFavoriteTimestamp() : 0;

            //입력된 정보를 Novel 객체 바구니에 담기(최초 등록 시 열람일은 "기록 없음", 좋아요는 false
            resultNovel = new Novel(title, author, genre, selectedPlatforms, folderPath,
                    coverPath, keywords, description, lastRead, isFav);

            //수정 모드인 경우 하트 링킹 타임스탬프도 유실되지 않도록 연속성을 보장
            resultNovel.setFavoriteTimestamp(favTime);
            resultNovel.setCompleted(cbCompleted.isSelected());

            dispose();
        });

        add(contentGridPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    //구조적 정렬을 위한 행 배치 생성 도구(텍스트 필드용)
    private JPanel createFormRow(String labelText, JTextField textField){
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBackground(Color.WHITE);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        textField.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        textField.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(label);
        row.add(Box.createVerticalStrut(5));
        row.add(textField);
        return row;
    }

    //구조적 정렬을 위한 행 배치 생성 도구(커스텀 패널용)
    private JPanel createFormRowWithComponent(String labelText, JComponent component){
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBackground(Color.WHITE);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        component.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(label);
        row.add(Box.createVerticalStrut(5));
        row.add(component);
        return row;
    }

    //메인 창에 등록된 소설 데이터를 꺼내갈 수 있도록 제공하는 메서드
    public Novel getResultNovel(){
        return resultNovel;
    }


    //창이 오픈 되는 그 시점에 최신 AppSettinss 데이터를 가로채 체크박스 그룹을 초기화 및 재생성
    private void buildDynamicPlatformCheckBoxes(JPanel panel, java.util.List<JCheckBox> list){
        System.out.println("플랫폼 목록:");

        //기존에 잔상으로 남아있던 컴포넌트와 수집 리스트를 청소
        panel.removeAll();
        list.clear();


        //현재 하드디스크 세이브 파일 및 메모리에 등록된 최신 플랫폼 목록을 순회하며 실시간 드로잉
        for(String platformName : AppSettings.getInstance().getCustomPlatforms()){
            JCheckBox chk = new JCheckBox(platformName);
            chk.setBackground(Color.WHITE);
            chk.setFont(new Font("맑은 고딕", Font.PLAIN, 12));

            //정보 수정 모드일 경우 체크 상태 복원도 연산도 이곳에서 동시 수행
            if(existingNovel != null && existingNovel.getPlatform().contains(platformName)){
                chk.setSelected(true);
            }

            panel.add(chk); //패널에 컴포넌트 즉시 도킹
            list.add(chk);  //데이터 수집 바구니에 적재
        }
        panel.revalidate();
        panel.repaint();
    }
}
