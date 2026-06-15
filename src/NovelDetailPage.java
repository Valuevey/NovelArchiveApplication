import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

public class NovelDetailPage {
    private JFrame detailFrame;
    private JPanel chapterListPanel;
    private JPanel pageTabPanel;        //페이징 버튼들이 들어갈 패널
    private Novel currentNovel;
    private ArrayList<File> txtFiles = new ArrayList<>();

    //원본 서재 인스턴스를 기억할 보관함 연결 통로 변수 생성
    private BookShelfPage parentShelf;

    //마지막으로 읽은 회차 번호를 기억할 변수(기본값 1화)
    private int lastReadChapter = 1;
    private boolean hasBookmark = false;

    //이어보기 버튼을 실시간으로 새로고침하기 위해 멤버 변수로 승격
    private JButton btnContinue;

    //사용자가 선택한 페이지 범위 관리 변수(기본값 : 1화부터 50화까지 분할 범위 지정)
    private int startPageRange = 0;
    private int endPageRange = 50;

    //정렬 필터 추적 변수(true : 1화부터, false: 최신순)
    private boolean isSortAscending = true;

    //현재 리스트가 전체 회차 목록(false)인지, 책갈피 메모 모드(true)인지 판별하는 스위치 플래그
    private boolean isBookmarkMode = false;

    // [조아라]식 작품 소개 토글 상태 변수(접었다 펼 수 있게)
    private boolean isDescExpanded = false;
    private JScrollPane descScrollPane;

    private JTextArea taDescription;

    //보관함에서 소설 데이터를 넘겨받아 창을 연다
    public void openDetailPage(Novel novel, BookShelfPage shelf){
        this.currentNovel = novel;
        this.parentShelf = shelf;   //주소 바인딩

        //1. 책갈피 정보를 먼저 로드하여 이어보기 회차 확인
        loadBookmarkInfo();

        detailFrame = new JFrame("소설 정보 - " + novel.getTitle());
        detailFrame.setSize(530, 750);
        detailFrame.setLocationRelativeTo(null);
        detailFrame.setLayout(new BorderLayout());

        //2. 상단 소설 상세 정보 구역(표지 + 메타데이터)
        JPanel topContainerPanel = new JPanel();
        topContainerPanel.setBackground(Color.WHITE);
        topContainerPanel.setLayout(new BoxLayout(topContainerPanel, BoxLayout.Y_AXIS));

        JPanel infoHeaderPanel = new JPanel(new BorderLayout(15, 0));
        infoHeaderPanel.setBackground(Color.WHITE);

        infoHeaderPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        //표지 이미지 처리
        JLabel lblCover = new JLabel();
        lblCover.setPreferredSize(new Dimension(110, 150));
        lblCover.setBorder(BorderFactory.createLineBorder(new Color(235, 235, 235)));
        lblCover.setHorizontalAlignment(SwingConstants.CENTER);

        lblCover.setOpaque(false); // 상자 배경 투명화
        lblCover.setBorder(BorderFactory.createEmptyBorder()); // 혹시 남아있을지 모를 사각 외곽선 삭제

        //상세 정보창 규격에 최적화된 표지 해상도 가로세로 공간 지정
        int targetWidth = 110;
        int targetHeight = 155;
        lblCover.setPreferredSize(new Dimension(targetWidth, targetHeight));
        lblCover.setMaximumSize(new Dimension(targetWidth, targetHeight));

        //소설 객체에 표지 경로가 존재하고 실제 파일이 하드디스크에 있는지 1차 검증
        String finalCoverPath = novel.getCoverPath();
        boolean hasUserCover = !finalCoverPath.isEmpty() && new File(finalCoverPath).exists();

        //만약 유저가 등록한 표지가 없다면, 준비된 no_cover.png파일을 fallback 대체재로 지정
        if(!hasUserCover){
            finalCoverPath = "C:\\novel\\icon\\no_cover.png";
        }

        //최종 결정된 이미지 경로(유저 표지 또는 기본 no_cover 템플릿)을 기반으로 고화질 스케일링 엔진 가동
        File targetFile = new File(finalCoverPath);
        if(targetFile.exists()){
            try{
                java.awt.image.BufferedImage srcImg = javax.imageio.ImageIO.read(targetFile);

                int srcWidth = srcImg.getWidth(null);
                int srcHeight = srcImg.getHeight(null);

                //종횡비 보존형 정밀 해상도 축소 비율 산출
                double targetScale = Math.min((double) targetWidth / srcWidth, (double) targetHeight / srcHeight);
                int scaledWidth = (int) (srcWidth * targetScale);
                int scaledHeight = (int) (srcHeight * targetScale);

                int x = (targetWidth - scaledWidth) / 2;
                int y = (targetHeight - scaledHeight) / 2;

                java.awt.image.BufferedImage resizedImg = new java.awt.image.BufferedImage(
                        targetWidth, targetHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB
                );
                Graphics2D g2 = resizedImg.createGraphics();

                //초고화질 보정 렌더링 다중 힌트 주입
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                //이미지 크기만큼 라운드 사각형 객체를 클리핑 영역으로 지정
                java.awt.geom.RoundRectangle2D roundedRectangle = new java.awt.geom.RoundRectangle2D.Float(
                        x, y, scaledWidth, scaledHeight, 16, 16
                );
                g2.setClip(roundedRectangle);

                g2.drawImage(srcImg, x, y, scaledWidth, scaledHeight, null);
                g2.dispose();

                lblCover.setIcon(new ImageIcon(resizedImg));
            } catch(Exception e){
                //이미지 디코딩 실패 시 텍스트 가드 작용
                lblCover.setText("IMAGE ERROR");
                lblCover.setFont(new Font("맑은 고딕", Font.BOLD, 11));
                lblCover.setForeground(Color.RED);
            }
        } else{
            //no_cover.png 파일 조차 디렉터리에 배치되지 않았을 때는 대비한 최하위 안전장치
            lblCover.setText("NO COVER");
            lblCover.setFont(new Font("맑은 고딕", Font.BOLD, 12));
            lblCover.setForeground(Color.LIGHT_GRAY);
        }


        //소설 정보창 표지 이미지 클릭 시 원본 크기 확대 모달 팝업 이벤트
        lblCover.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblCover.setToolTipText("클릭하면 원본 표지가 크게 펼쳐집니다");

        lblCover.addMouseListener(new java.awt.event.MouseAdapter(){
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e){
                String coverPath = novel.getCoverPath();
                boolean hasUserCover = coverPath != null && !coverPath.isEmpty() && new File(coverPath).exists();

                if(!hasUserCover){
                    coverPath = "C:\\novel\\icon\\no_cover.png";
                }
                File targetFile = new File(coverPath);

                if(targetFile.exists()){
                    ImageIcon originalIcon = new ImageIcon(coverPath);
                    int imgWidth = originalIcon.getIconWidth();
                    int imgHeight = originalIcon.getIconHeight();

                    //부모창(detailFrame)을 기반으로 하는 모달 다이얼로그 개설
                    JDialog viewerDialog = new JDialog(detailFrame, "원본 표지 미리보기", true);
                    viewerDialog.getContentPane().setBackground(Color.WHITE);
                    viewerDialog.setLayout(new BorderLayout());

                    //모니터 해상도 한계선 가드 락 레이아웃 연산 (최대 가로 800, 세로 900제한)
                    if(imgWidth > 400 || imgHeight < 550){
                        double scale = Math.min(400.0 / imgWidth, 550.0 / imgHeight);
                        imgWidth = (int) (imgWidth * scale);
                        imgHeight = (int) (imgHeight * scale);
                        Image scaledImg = originalIcon.getImage().getScaledInstance(imgWidth, imgHeight, Image.SCALE_SMOOTH);
                        originalIcon = new ImageIcon(scaledImg);
                    }

                    //확대창 이미지 모서리 깍아내기
                    final int finalWidth = imgWidth;
                    final int finalHeight = imgHeight;
                    final ImageIcon finalIcon = originalIcon;

                    JLabel lblFullImage = new JLabel() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                            // 팝업 본판 이미지 크기에 완벽하게 밀착하는 라운드 클리핑 락 가동
                            java.awt.geom.RoundRectangle2D roundedRectangle = new java.awt.geom.RoundRectangle2D.Float(
                                    0, 0, getWidth(), getHeight(), 16, 16
                            );
                            g2.setClip(roundedRectangle);

                            g2.drawImage(finalIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
                            g2.dispose();
                        }
                    };

                    // JLabel 본체 프레임 사각선 격리 지우개 처리
                    lblFullImage.setOpaque(false);
                    lblFullImage.setBorder(BorderFactory.createEmptyBorder());
                    lblFullImage.setPreferredSize(new Dimension(imgWidth, imgHeight));
                    lblFullImage.setHorizontalAlignment(SwingConstants.CENTER);

                    //다시 클릭하면 원본 창이 닫히는 리스너 가동
                    lblFullImage.addMouseListener(new java.awt.event.MouseAdapter(){
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e){
                            viewerDialog.dispose();
                        }
                    });
                    // 다이얼로그 외곽 프레임 투명화 마감 (윈도우 모서리 직각 찌꺼기 완벽 차단)
                    viewerDialog.setUndecorated(true); // 타이틀바 제거로 미니멀 극대화
                    viewerDialog.setBackground(new Color(0, 0, 0, 0)); // 배경 투명화

                    viewerDialog.add(lblFullImage, BorderLayout.CENTER);
                    viewerDialog.pack(); // 컴포넌트 실제 크기에 맞게 창 크기 자동 최적화 마감
                    viewerDialog.setLocationRelativeTo(detailFrame);
                    viewerDialog.setVisible(true);
                }
            }
        });

        //텍스트 메타데이터 처리(제목, 작가, 플랫폼)
        JPanel textMetaPanel = new JPanel(new BorderLayout());
        textMetaPanel.setBackground(Color.WHITE);

        JPanel centerInfoBox = new JPanel();
        centerInfoBox.setBackground(Color.WHITE);
        centerInfoBox.setLayout(new BoxLayout(centerInfoBox, BoxLayout.Y_AXIS));

        //제목 출력
        //글자 수 길이에 따라 폰트 크기를 20 또는 16으로 자동 판별
        String titleText = novel.getTitle();
        int titleFontSize = (titleText.length() > 20) ? 16 : 20;    //20자 초과 시 글자 크기를 16으로 줄임

        JLabel lblTitle = new JLabel("<html><body style='width: 240px;'>" + novel.getTitle() + "</body></html>");
        lblTitle.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        //가로 260, 세로 65를 줘서 2~3줄짜리 제목 높이가 잘림 없이 아래 라벨을 밀어내며 전부 출력되도록 공간 확보
        lblTitle.setPreferredSize(new Dimension(260, 65));
        lblTitle.setMinimumSize(new Dimension(260, 65));

        //장르 및 작가 이름 레이아웃 보관함 정보 규격으로 일치화 정정
        String genreStr = (novel.getGenre() == null || novel.getGenre().isEmpty() ? "미분류" : novel.getGenre());
        String authorStr = (novel.getAuthor() == null || novel.getAuthor().isEmpty() ? "작자미상" : novel.getAuthor());
        JLabel lblGenreAuthor = new JLabel(genreStr + " ㆍ " + authorStr);
        lblGenreAuthor.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        lblGenreAuthor.setForeground(new Color(80, 80, 80));
        lblGenreAuthor.setAlignmentX(Component.LEFT_ALIGNMENT);

        //작가 라벨 마우스 링크 리스너 이식(클릭 시 메인 서재 작가별 검색 연동)
        lblGenreAuthor.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblGenreAuthor.setToolTipText("클릭하면 이 작가의 다른 작품을 검색합니다.");
        lblGenreAuthor.addMouseListener(new java.awt.event.MouseAdapter(){
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e){
                if(parentShelf != null && !authorStr.equals("작자미상")){
                    //메인 화면을 [내 서재] 기본 메뉴 상태로 원복
                    parentShelf.triggerMyLibraryMenu();

                    //메인 서재 검색창 주소에 작가명 바인딩 발사
                    parentShelf.searchByAuthor("@" + authorStr);

                    //현재 상세창은 클로즈 반환
                    detailFrame.dispose();
                }
            }
        });

        JLabel lblPlatform = new JLabel("플랫폼: " + novel.getPlatform());
        lblPlatform.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        lblPlatform.setForeground(new Color(0, 100, 100));  //푸른색 강조
        lblPlatform.setAlignmentX(Component.LEFT_ALIGNMENT);

        //총 편수 표시
        readChapterFiles();
        JLabel lblTotalCount = new JLabel("총 " + txtFiles.size() + "화");
        lblTotalCount.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        lblTotalCount.setForeground(new Color(120, 120, 120));
        lblTotalCount.setAlignmentX(Component.LEFT_ALIGNMENT);

        //키워드 라벨 레이어 추가
        String keywordStr = (novel.getKeywords() == null || novel.getKeywords().isEmpty() ? "없음" : novel.getKeywords());
        // 쉼표로 쪼개서 샵(#) 기호와 연동
        String[] splitKeys = keywordStr.split(",");
        StringBuilder sbKeys = new StringBuilder();
        for(String key : splitKeys){
            if(!key.trim().isEmpty()) sbKeys.append("#").append(key.trim()).append(" ");
        }
        JLabel lblKeywords = new JLabel(sbKeys.toString().isEmpty() ? "#키워드 없음" : sbKeys.toString());
        lblKeywords.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        lblKeywords.setForeground(new Color(0, 140, 180));
        lblKeywords.setAlignmentX(Component.LEFT_ALIGNMENT);

        centerInfoBox.add(lblTitle);
        centerInfoBox.add(Box.createVerticalStrut(5));
        centerInfoBox.add(lblGenreAuthor);
        centerInfoBox.add(Box.createVerticalStrut(3));
        centerInfoBox.add(lblPlatform);
        centerInfoBox.add(Box.createVerticalStrut(3));
        centerInfoBox.add(lblTotalCount);
        centerInfoBox.add(Box.createVerticalStrut(5));
        centerInfoBox.add(lblKeywords);

        //1. 점 3개 클릭 시 아래로 떨어질 드롭다운 팝업 메뉴 상자
        JPopupMenu moreMenu = new JPopupMenu();
        moreMenu.setBackground(Color.WHITE);
        moreMenu.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 230), 1));

        //팝업 메뉴 내무 항목 A : 수정 메뉴 세팅
        JMenuItem menuEdit = new JMenuItem("작품 정보 수정");
        menuEdit.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        menuEdit.setBackground(Color.WHITE);
        menuEdit.addActionListener(e -> {
            //수정 전용 팝업 창을 띄우고 현재 소설 객체를 전달
            AddNovelDialog editDialog = new AddNovelDialog(detailFrame, currentNovel);
            editDialog.setVisible(true);

            //사용자가 수정을 완료하고 팝업을 닫았을 때 결과 수집
            Novel editedResult = editDialog.getResultNovel();
            if(editedResult != null) {
                //1. 기존 데이터 객체의 필드값들ㅇ르 새 정보로 정밀 덮어쓰기
                currentNovel.setTitle(editedResult.getTitle());
                currentNovel.setAuthor(editedResult.getAuthor());
                currentNovel.setGenre(editedResult.getGenre());
                currentNovel.setPlatforms(editedResult.getPlatform());
                currentNovel.setFolderPath(editedResult.getFolderPath());
                currentNovel.setCoverPath(editedResult.getCoverPath());
                currentNovel.setKeywords(editedResult.getKeywords());
                currentNovel.setDescription(editedResult.getDescription());
                currentNovel.setCompleted(editedResult.isCompleted());


                //2. 메인 스토리지 파일 영구 백업 동기화 및 메인 서재 리프레시
                if (parentShelf != null) {
                    parentShelf.saveLibraryData();
                    parentShelf.refreshLibrary();
                }

                //3. 현재 열려있는 상세창의 UI 데이터들도 실시간 갱신 처리
                //정보 수정 직후에도 글자 수를 즉시 판별하여 폰트 크기를 재조정
                String editTitleText = currentNovel.getTitle();
                int editTitleFontSize = (editTitleText.length() > 20) ? 16 : 20;

                lblTitle.setFont(new Font("맑은 고딕", Font.BOLD, editTitleFontSize));
                lblTitle.setText("<html><body style='width: 240px;'>" + editTitleText + "</body></html>");

                String updatedGenre = (currentNovel.getGenre() == null ||
                        currentNovel.getGenre().isEmpty() ? "미분류" : currentNovel.getGenre());
                String updatedAuthor = (currentNovel.getAuthor() == null ||
                        currentNovel.getAuthor().isEmpty() ? "작자미상" : currentNovel.getAuthor());
                lblGenreAuthor.setText(updatedGenre + "ㆍ" + updatedAuthor);

                lblPlatform.setText("플랫폼: " + currentNovel.getPlatform());
                taDescription.setText(currentNovel.getDescription().isEmpty() ? "등록된 작품 소개글이 없습니다." : currentNovel.getDescription());

                //키워드 # 마킹 복원 동기화
                String[] editSplitKeys = currentNovel.getKeywords().split(",");
                StringBuilder editSbKeys = new StringBuilder();
                for (String key : editSplitKeys) {
                    if (!key.trim().isEmpty()) editSbKeys.append("#").append(key.trim()).append(" ");
                }
                lblKeywords.setText(sbKeys.toString().isEmpty() ? "#키워드 없음" : sbKeys.toString());

                //표지 이미지 변경 실시간 감지 리렌더링
                if (currentNovel.getCoverPath() != null && !currentNovel.getCoverPath().isEmpty() && new File(currentNovel.getCoverPath()).exists()) {
                    ImageIcon origIcon = new ImageIcon(currentNovel.getCoverPath());
                    Image scaledImg = origIcon.getImage().getScaledInstance(110, 150, Image.SCALE_SMOOTH);
                    lblCover.setIcon(new ImageIcon(scaledImg));
                    lblCover.setText("");
                } else {
                    lblCover.setIcon(null);
                    lblCover.setText("NO COVER");
                    lblCover.setForeground(Color.GRAY);
                }
                //텍스트가 늘어난 만큼 부모 컨테이너들의 라이아웃 좌표를 실시간으로 재계산하여 화면에 출력
                lblTitle.revalidate();
                lblTitle.repaint();
                centerInfoBox.revalidate();
                centerInfoBox.repaint();
                infoHeaderPanel.revalidate();
                infoHeaderPanel.repaint();
                topContainerPanel.revalidate();
                topContainerPanel.repaint();

                JOptionPane.showMessageDialog(detailFrame, "작품 정보가 성공적으로 수정되었습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        //팝업 메뉴 내부 항목 B : 삭제 메뉴 세팅
        JMenuItem menuDelete = new JMenuItem("작품 완전 삭제");
        menuDelete.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        menuDelete.setBackground(Color.WHITE);
        menuDelete.setForeground(Color.RED);    //삭제 강조는 적색 매핑
        menuDelete.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(detailFrame,
                    "정말로 이 작품을 보관함에서 삭제하시겠습니까?\n(실제 소설 텍스트 파일은 삭제 되지 않습니다.)",
                    "작품 삭제", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if(choice == JOptionPane.YES_OPTION){
                if(parentShelf != null){
                    parentShelf.deleteNovel(currentNovel);
                }
                detailFrame.dispose();
            }
        });

        //팝업 메뉴 내부 항목 C : 메모 기능
        JMenuItem memuSummary = new JMenuItem("회차별 요약 메모");
        memuSummary.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        memuSummary.setBackground(Color.WHITE);
        memuSummary.addActionListener(e -> {
            //메모 편집 및 조회를 위한 독립형 팝업 다이얼로그 구동 메서드 호출
            openSummaryNoteDialog();
        });

        //메뉴 상자에 항목 부착 조립
        moreMenu.add(memuSummary);          //메모 기능
        moreMenu.add(new JSeparator());     //실선 구분 바
        moreMenu.add(menuEdit);             //작품 수정 기능
        moreMenu.add(new JSeparator());
        moreMenu.add(menuDelete);           //작품 삭제 기능

        //2. 우측 컨트롤 타워 패널 배치 엔진 가동
        JPanel controlBtnRow = new JPanel(new GridBagLayout());
        controlBtnRow.setBackground(Color.WHITE);
        GridBagConstraints cbc = new GridBagConstraints();
        cbc.weightx = 0.0;
        cbc.weighty = 0.0;

        //[1층 0열]: 더보기 [ ⋮ ] 그래픽스 버튼 컴포넌트 선언
        JButton btnMore = new JButton();
        btnMore.setFocusPainted(false);
        btnMore.setBorderPainted(false);
        btnMore.setContentAreaFilled(false);
        btnMore.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMore.setToolTipText("작품 관리 메뉴 열기");

        //유니코드 깨짐 완전 가드 : 점 3개 수직 정렬 드로잉 인쇄 렌더링
        btnMore.setIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(120, 130, 140));  //모던 다크그레이 컬러

                //수직 정렬 점 3개 정밀 인쇄 연산
                g2.fillOval(x+7, y+2, 3, 3);
                g2.fillOval(x+7, y+7, 3, 3);
                g2.fillOval(x+7, y+12, 3, 3);
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
        });

        //더보기 버튼 누르면 바로 하단부 좌표에 팝업 메뉴창 정밀 바인딩 오픈
        btnMore.addActionListener(e -> {
            moreMenu.show(btnMore, 0, btnMore.getHeight());
        });

        //[2층 0열] : 좋아요 하트 토글 버튼 속성 리세팅
        JButton btnFavorite = new JButton();
        btnFavorite.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
        btnFavorite.setFocusPainted(false);
        btnFavorite.setBorderPainted(false);
        btnFavorite.setContentAreaFilled(false);
        btnFavorite.setCursor(new Cursor(Cursor.HAND_CURSOR));

        Icon filledHeartIcon = new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 65, 90)); // 화사한 삼홍색 하트 컬러

                java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
                path.moveTo(x + 9, y + 4.5);
                // 좌측 곡선 텐션 제어
                path.curveTo(x + 9, y + 1, x + 2, y + 1, x + 2, y + 5.5);
                path.curveTo(x + 2, y + 10, x + 6, y + 13, x + 9, y + 16);
                // 우측 곡선 텐션 제어 (완벽 대칭 마킹)
                path.curveTo(x + 12, y + 13, x + 16, y + 10, x + 16, y + 5.5);
                path.curveTo(x + 16, y + 1, x + 9, y + 1, x + 9, y + 4.5);
                path.closePath();

                g2.fill(path);
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

        Icon emptyHeartIcon = new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 외곽선이 흐리게 보이던 문제를 해결하기 위해 다크 그레이로 선명도 보정
                g2.setColor(new Color(160, 165, 175));
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
                path.moveTo(x + 9, y + 4.5);
                path.curveTo(x + 9, y + 1, x + 2, y + 1, x + 2, y + 5.5);
                path.curveTo(x + 2, y + 10, x + 6, y + 13, x + 9, y + 16);
                path.curveTo(x + 12, y + 13, x + 16, y + 10, x + 16, y + 5.5);
                path.curveTo(x + 16, y + 1, x + 9, y + 1, x + 9, y + 4.5);
                path.closePath();

                g2.draw(path);
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

        btnFavorite.setText("");
        btnFavorite.setIcon(novel.isFavorite() ? filledHeartIcon : emptyHeartIcon);

        for(java.awt.event.ActionListener al : btnFavorite.getActionListeners()){
            btnFavorite.removeActionListener(al);
        }

        btnFavorite.addActionListener(e -> {
            novel.setFavorite(!novel.isFavorite());

            //하트를 켰을 때는 현재 시각(System.currentTimeMillis())을 각인하고, 껐을 대는 0으로 초기화
            if(novel.isFavorite()){
                btnFavorite.setIcon(filledHeartIcon);
                novel.setFavoriteTimestamp(System.currentTimeMillis());
            } else{
                btnFavorite.setIcon(emptyHeartIcon);
                novel.setFavoriteTimestamp(0);
            }
            if(parentShelf != null) parentShelf.saveLibraryData();
        });

        //[1층]: 우측 맨 상단 구석탱이에 더보기 버튼 도킹 시키기
        cbc.gridx = 0;
        cbc.gridy = 0;
        cbc.anchor = GridBagConstraints.NORTHEAST;
        cbc.insets = new Insets(0, 0, 0, 0);
        controlBtnRow.add(btnMore, cbc);

        //[2층]: 더보기 버튼 정밀 수직 하단 구역에 하트 고정 배치
        cbc.gridx = 0;
        cbc.gridy = 1;
        cbc.anchor = GridBagConstraints.EAST;
        cbc.insets = new Insets(35, 0, 0, 0);
        controlBtnRow.add(btnFavorite, cbc);

        textMetaPanel.add(centerInfoBox, BorderLayout.CENTER);
        textMetaPanel.add(controlBtnRow, BorderLayout.EAST);

        infoHeaderPanel.add(lblCover, BorderLayout.WEST);
        infoHeaderPanel.add(textMetaPanel, BorderLayout.CENTER);

        //HTML 형식을 활용해 타이틀과 본문을 한 그릇에 담음
        // '작품 소개' 타이틀의 폰트 크기를 12pt로 인상하여 확실한 제목 가시성을 확보합니다.
        String descTitle = "<span style='color: rgb(0, 120, 120); font-weight: bold; font-size: 12pt;'>작품 소개</span><br>";

        String rawBody = novel.getDescription().isEmpty() ? "등록된 작품 소개글이 없습니다" : novel.getDescription(); //
        String collapsedBody = rawBody.replace("\n", " ").trim();
        if (collapsedBody.length() > 40) {
            collapsedBody = collapsedBody.substring(0, 40) + "...";
        }

        // HTML 태그 인식을 위한 JTextPane 인터페이스 선언 및 생성
        JTextPane htmlDescription = new JTextPane(); //
        htmlDescription.setContentType("text/html"); //

        // 가로 방향 팽창 및 스크롤바 생성을 근본적으로 차단하기 위해 nowrap 스타일을 삭제하고 안전하게 문장을 주입합니다.
        htmlDescription.setText("<html><body style='font-family: 맑은 고딕; font-size: 10.5pt; color: rgb(65, 70, 80); line-height: 1.4;'>"
                + descTitle + collapsedBody + "</body></html>");

        htmlDescription.setEditable(false);
        htmlDescription.setFocusable(false);
        htmlDescription.setHighlighter(null);
        htmlDescription.setOpaque(false);

        htmlDescription.setCaretPosition(0);

        //껍데기 역할을 할 JScrollPane을 오버라이드하여 배경과 청록색 수직 기둥 선을 직접 드로잉
        descScrollPane = new JScrollPane(htmlDescription){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 1. 연회색 라운드 배경 상자
                g2.setColor(new Color(245, 247, 249));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                // 2. 좌측 벽면에 청록색 수직선 배치
                g2.setColor(new Color(0, 140, 140));
                g2.fillRoundRect(0, 8, 3, getHeight()-16, 2, 2);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        //처음 구동 시에는 무조건 접힌 상태(높이 35픽셀로 압축)
        descScrollPane.setPreferredSize(new Dimension(460, 60));
        descScrollPane.setOpaque(false);
        descScrollPane.getViewport().setOpaque(false);

        descScrollPane.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 14));
        descScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        descScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        htmlDescription.setCursor(new Cursor(Cursor.HAND_CURSOR));

        final String finalCollapsedBody = collapsedBody;        //익명 클래스 내부 참조 바인딩 가드
        //작품 소개글 영역 클릭 액션 리스너 바인딩
        MouseAdapter toggleMouseEngine = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isDescExpanded = !isDescExpanded;
                if(isDescExpanded){ //
                    descScrollPane.setPreferredSize(new Dimension(460, 160)); //
                    descScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED); //


                    // 펼쳐졌을 때는 원문의 줄바꿈(\n)을 정상적으로 <br> 태그로 치환해 복원 출력합니다.
                    String expandedTitle = "<span style='color: rgb(0, 120, 120); font-weight: bold; font-size: 12pt;'>작품 소개</span><br><br>";
                    String expandedBody = rawBody.replace("\n", "<br>");
                    htmlDescription.setText("<html><body style='font-family: 맑은 고딕; font-size: 10.5pt; color: rgb(65, 70, 80); line-height: 1.4;'>"
                            + expandedTitle + expandedBody + "</body></html>");

                    htmlDescription.setCaretPosition(0);
                } else{ //
                    descScrollPane.setPreferredSize(new Dimension(460, 60)); //
                    descScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER); //

                    // 축소 시 사전에 정교하게 커팅해 둔 단일 한 줄 말줄임표 포맷으로 되돌립니다.
                    htmlDescription.setText("<html><body style='font-family: 맑은 고딕; font-size: 10.5pt; color: rgb(65, 70, 80); line-height: 1.4;'>"
                            + descTitle + finalCollapsedBody + "</body></html>");
                    htmlDescription.setCaretPosition(0); //
                }
                //실시간 Swing 레이아웃 디바이드 갱신 지시
                topContainerPanel.revalidate();
                topContainerPanel.repaint();
            }
        };
        htmlDescription.addMouseListener(toggleMouseEngine);

        JPanel descWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        descWrapper.setBackground(Color.WHITE);
        descWrapper.add(descScrollPane);

        // 50화 단위 페이지 분할 버튼 바 패널 세팅
        //정렬 버튼과 페이지 숫자 버튼을 한 행에 배치
        JPanel unifiedControlBar = new JPanel(new BorderLayout());
        unifiedControlBar.setBackground(Color.WHITE);
        unifiedControlBar.setBorder(BorderFactory.createEmptyBorder(12, 16, 10, 16));

        // 처음 로드 시 '최신순'으로 노출되도록 초기 클래그를 false(역순)으로 설정
        //[좌측 구역]: 원버튼 토글 필터 배치
        isSortAscending = false;

        JButton btnSortToggle = new JButton("↕ 최신순");
        btnSortToggle.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        btnSortToggle.setFocusPainted(false);
        btnSortToggle.setBorderPainted(false);      //외곽 테두리 선 제거
        btnSortToggle.setContentAreaFilled(false);  //배경 삭제
        btnSortToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSortToggle.setForeground(new Color(80, 140, 140));

        btnSortToggle.addActionListener(e -> {
            //버튼을 클릭할 때마다 상태 정렬 스위치를 반전(true <-> false)
            isSortAscending = !isSortAscending;
            if(isSortAscending){
                btnSortToggle.setText("↕ 1화부터");
            } else{
                btnSortToggle.setText("↕ 최신순");
            }
            //바뀐 정렬 기준으로 회차 목록 리패킹 렌더링 가동
            renderChapterList();
        });
        unifiedControlBar.add(btnSortToggle, BorderLayout.WEST);

        //정렬 버튼 우측 옆구리에 나란히 서게 될 [책갈피 목록 보기] 토글 단추 배포
        JButton btnBookmarkToggle = new JButton("책갈피 보기");
        btnBookmarkToggle.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        btnBookmarkToggle.setFocusPainted(false);
        btnBookmarkToggle.setBorderPainted(false);
        btnBookmarkToggle.setContentAreaFilled(false);
        btnBookmarkToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBookmarkToggle.setForeground(new Color(140, 145, 155));

        btnBookmarkToggle.addActionListener(evt -> {
            isBookmarkMode = !isBookmarkMode;   //상태 반전
            if(isBookmarkMode){
                btnBookmarkToggle.setText("전체 회차 보기");
                btnBookmarkToggle.setForeground(new Color(0, 140, 140));
                btnSortToggle.setForeground(new Color(180, 140, 140));
                btnSortToggle.setEnabled(true);    //책갈피 모드에서 회차 정렬 버튼 사용 가능
            } else{
                btnBookmarkToggle.setText("책갈피 보기");
                btnBookmarkToggle.setForeground(new Color(140, 145, 155));
                btnSortToggle.setForeground(new Color(0, 140, 140));
                btnSortToggle.setEnabled(true);
            }
            renderChapterList();    //바뀐 스위치 플래그 기반 하단 바둑판 리렌더링
        });

        //좌측 필터 패널에 정렬 버튼과 책갈피 버튼을 가로로 정착하여 부착
        JPanel leftFilterGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftFilterGroup.setBackground(Color.WHITE);
        leftFilterGroup.add(btnSortToggle);
        leftFilterGroup.add(btnBookmarkToggle);
        unifiedControlBar.add(leftFilterGroup, BorderLayout.WEST);

        pageTabPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        pageTabPanel.setBackground(Color.WHITE);
        unifiedControlBar.add(pageTabPanel, BorderLayout.EAST);

        //[우측 구역]: 숫자 형태의 웹 스타일 페이징 탭 배치
        topContainerPanel.add(infoHeaderPanel);
        topContainerPanel.add(descWrapper);
        topContainerPanel.add(unifiedControlBar);   //통일된 단일 바를 부착

        //3. 중앙 회차 목록 구역(스크롤바 포함)
        chapterListPanel = new JPanel();
        chapterListPanel.setBackground(Color.WHITE);
        chapterListPanel.setLayout(new BoxLayout(chapterListPanel, BoxLayout.Y_AXIS));
        chapterListPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JScrollPane scrollPane = new JScrollPane(chapterListPanel);
        //테두리 만들기
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(235, 235, 235)));

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        //전체 파일 개수를 파악한 뒤 페이징 탭 버튼 생성
        generatePageTabs();
        //필터링 범위에 맞는 회차 목록 출력
        renderChapterList();

        //4. 하단 고정 이어보기 패널 영역
        JPanel bottomStickyPanel = new JPanel(new BorderLayout());
        bottomStickyPanel.setBackground(Color.WHITE);

        //위쪽에만 얇은 구분선 추가
        bottomStickyPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));

        //버튼 생성 로직 분리
        btnContinue = new JButton();
        btnContinue.setBackground(new Color(0, 140, 140));
        btnContinue.setForeground(Color.WHITE);
        btnContinue.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        btnContinue.setFocusPainted(false);
        btnContinue.setBorder(BorderFactory.createEmptyBorder(13, 0, 13, 0));   //버튼 높이 확보

        //초기 텍스트 세팅
        updateContinueButtonText();

        //이어보기 버튼 클릭 시 뷰어로 다이렉트 연결
        btnContinue.addActionListener(e -> {
            //회차 목록이 비어있다면 경고창을 띄우고 리턴(종료)하여 튕김 현상 방지
            if(txtFiles == null || txtFiles.isEmpty()){
                JOptionPane.showMessageDialog(detailFrame, "불러올 회차 파일이 존재하지 않습니다.", "경고", JOptionPane.WARNING_MESSAGE);
                return;
            }
            NovelViewerPage viewer = new NovelViewerPage();
            //기록된 회차 번호를 주입하여 오픈
            viewer.openViewer(currentNovel.getFolderPath(), lastReadChapter, this, currentNovel);
        });

        bottomStickyPanel.add(btnContinue, BorderLayout.CENTER);

        //5. 레이아웃 조립(스크롤 패널과 하단 바 패널을 완벽히 격리 분리
        detailFrame.add(topContainerPanel, BorderLayout.NORTH);     //정보창+페이징바가 합쳐진 컨테이너 배치
        detailFrame.add(scrollPane, BorderLayout.CENTER);
        detailFrame.add(bottomStickyPanel, BorderLayout.SOUTH); //남쪽에 배치하여 고정

        detailFrame.setVisible(true);
    }

    //외부(뷰어 창)에서 책갈피를 새로 읽고 하단 고정 버튼을 갱신할 수 있도록 열어두는 리프레시 메서드
    public void refreshBookmarkStatus(){
        loadBookmarkInfo();
        updateContinueButtonText();
    }

    //버튼 텍스트만 실시간으로 교체해주는 메서드
    private void updateContinueButtonText(){
        //책갈피 존재 유무에 따라 버튼 텍스트 변경
        String buttonText = hasBookmark ? "▶ 이어보기 (" + lastReadChapter + "화)" : "▶ 첫 화 보기 (1화)";
        btnContinue.setText(buttonText);
    }

    //소설 폴더 내의 .txt 파일들을 스캔하여 정렬하는 로직
    private void readChapterFiles(){
        txtFiles.clear();   //데이터 비우기
        File dir = new File(currentNovel.getFolderPath());

        if(dir.exists() && dir.isDirectory()){
            File[] files = dir.listFiles((dir1, name) -> {
                String lowerName = name.toLowerCase();
                return lowerName.endsWith(".txt")
                        && !lowerName.equals("bookmark.txt")
                        && !lowerName.equals("memo_bookmarks.txt")
                        && !lowerName.startsWith("summary_notes");  //메모장 파일 스캔 원천 차단
            });
            if(files != null){
                for(File f : files){
                    txtFiles.add(f);
                }
            }
        }
    }

    //전체 소설 파일 개수를 추적하여 50화 단위 분할 버튼을 생성하는 메서드
    private void generatePageTabs(){
        pageTabPanel.removeAll();

        if(txtFiles.isEmpty()) return;

        //가장 마지막 파일의 회차 번호를 구함(리스트의 마지막 항목 활용)
        int totalMaxChapter = txtFiles.size();
        int pageNumber = 1;

        //50화 단위로 루프를 돌며 필요한 만큼 탭 버튼을 동적으로 출력
        //ex. 105화까지 있다면, 1~50, 51~100, 101~50 범위의 버튼을 노출
        for(int i = 1; i <= totalMaxChapter; i+=50){
            final int start = i;
            final int end = i+49;
            final int currentPageNum = pageNumber;

            //숫자 형태의 버튼 생성
            JButton btnTab = new JButton(String.valueOf(pageNumber));
            btnTab.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            btnTab.setFocusPainted(false);
            btnTab.setBorderPainted(false);     //외곽 테두리 선 제거
            btnTab.setContentAreaFilled(false);     //배경 채우기 제거
            btnTab.setCursor(new Cursor(Cursor.HAND_CURSOR));

            btnTab.setMargin(new Insets(0, 0, 0, 0));

            //현재 선택된 페이지 번호 강조 마킹 연산
            if(this.startPageRange == start){
                btnTab.setForeground(new Color(0, 120, 230));
                btnTab.setFont(new Font("맑은 고딕", Font.BOLD, 13));
            } else{
                btnTab.setForeground(Color.GRAY);
            }

            //페이징 버튼 클릭 시 타겟 범위 변수를 변경하고 리스트를 다시 재렌더링하는 트리거 연결
            btnTab.addActionListener(e -> {
                this.startPageRange = start;
                this.endPageRange = end;
                generatePageTabs();     //버튼 강조 상태 갱신을 위해 탭 다시 그리기
                renderChapterList();    //본문 리스트 다시 필터링 출력
            });

            pageTabPanel.add(btnTab);

            //숫자 사이에 구분 벽 기호(|) 추가 (단, 마지막 번호 뒤에는 생략)
            if(i + 50 <= totalMaxChapter){
                JLabel lblDivider = new JLabel("|");
                lblDivider.setFont(new Font("맑은 고딕", Font.PLAIN, 10));
                lblDivider.setForeground(new Color(210, 215, 220));
                pageTabPanel.add(lblDivider);
            }
            pageNumber++;
        }
        pageTabPanel.revalidate();
        pageTabPanel.repaint();
    }

    //정렬된 파일 목록을 바탕으로 회차 리스트 버튼 UI를 그리는 로직
    public void renderChapterList(){
        chapterListPanel.removeAll();

        //[트랙 A]: 사용자가 책갈피 보기 버튼을 눌러 스위칭이 켜진 경우 가동
        if(isBookmarkMode){
            //책갈피 파일 경로 생성
            String path = currentNovel.getFolderPath();
            //폴더 끝에 \가 없으면 추가
            if(!path.endsWith(File.separator)){
                path += File.separator;
            }
            File memoFile = new File(path + "memo_bookmarks.txt");
            if(!memoFile.exists()){
                JLabel lblEmpty = new JLabel("저장된 책갈피 메모가 없습니다.", SwingConstants.CENTER);
                lblEmpty.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                lblEmpty.setForeground(Color.GRAY);
                chapterListPanel.add(lblEmpty);
            } else{
                //메모 목록을 메모리에 수집한 뒤 회차 정렬 필터(isSortAscending)에 맞게 재정렬하는 엔진
                java.util.List<String[]> bookmarkLines = new java.util.ArrayList<>();
                try(BufferedReader br = new BufferedReader(new FileReader(memoFile))){
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] split = line.split("\\|", -1);
                        if(split.length >= 2){
                            bookmarkLines.add(split);}
                    }
                } catch(Exception ex){
                    System.out.println("메모장 로드 실패: " + ex.getMessage());
                }

                //수집된 책갈피 컬렉션을 현재 활성화된 순방향/역방향 정렬 조건 스위치 변수에 의거하여 소팅 연산
                Collections.sort(bookmarkLines, (b1, b2) -> {
                    try{
                        int num1 = Integer.parseInt(b1[0].trim());
                        int num2 = Integer.parseInt(b2[0].trim());
                        return isSortAscending ? Integer.compare(num1, num2) : Integer.compare(num2, num1);
                    } catch(Exception e){
                        return b1[0].compareTo(b2[0]);
                    }
                });

                for(String[] split : bookmarkLines){
                    String chNumStr = split[0];
                    String memoText = split[1].trim();

                    JPanel row = new JPanel(new BorderLayout());
                    row.setBackground(Color.WHITE);
                    row.setMaximumSize(new Dimension(500, 38));
                    row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(242, 244, 246)));

                    //좌측 텍스트 그룹(회차 번호, 메모, 배지)을 수평 정렬선상에 얹기 위한 FlowLayout 컴포넌트
                    JPanel leftTitleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 4));
                    leftTitleGroup.setBackground(Color.WHITE);

                    //메모가 비어있을 때, "저장된 메모가 없습니다" 문구가 노출되도록 동적 가드 연산 처리
                    String finalShowText = memoText.isEmpty() ? "저장된 메모가 없습니다" : memoText;

                    //좌측 영역: 회차 번호 및 유저가 입력한 최대 40자의 한줄 요약 메모 인쇄
                    JLabel lblMemo = new JLabel("[" + chNumStr + "화] " + finalShowText);
                    //메모가 진짜 등록된 데이터인지 가짜 대체재 문구인지에 따라 글자 색상을 변환
                    if(memoText.isEmpty()){
                        lblMemo.setFont(new Font("맑은 고딕", Font.ITALIC, 13));    //ITALIC : 기울임
                        lblMemo.setForeground(Color.LIGHT_GRAY);    //흐린 회색
                    } else{
                        lblMemo.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
                        lblMemo.setForeground(new Color(60, 65, 70));   //본문 잉크색 지정
                    }

                    //메모와 제목 위에 마우스 올리면 손모양으로 변환
                    lblMemo.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    lblMemo.setToolTipText("클릭하면 이 책갈피 회차로 즉시 이동합니다.");

                    final int targetch = Integer.parseInt(chNumStr);

                    MouseAdapter allInOneLinkEngine = new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            NovelViewerPage viewer = new NovelViewerPage();
                            viewer.openViewer(currentNovel.getFolderPath(), targetch, NovelDetailPage.this, currentNovel);
                        }
                    };
                    lblMemo.addMouseListener(allInOneLinkEngine);
                    leftTitleGroup.add(lblMemo);

                    JButton btnRead = new JButton("보기");
                    btnRead.setFont(new Font("맑은 고딕", Font.BOLD, 12));
                    btnRead.setFocusPainted(false);
                    btnRead.setBorderPainted(false);
                    btnRead.setContentAreaFilled(false);
                    btnRead.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    btnRead.setForeground(new Color(0, 140, 140));
                    btnRead.setMargin(new Insets(2, 8, 2, 8));

                    btnRead.addActionListener(e -> {
                        NovelViewerPage viewer = new NovelViewerPage();
                        viewer.openViewer(currentNovel.getFolderPath(), targetch, this, currentNovel);
                    });

                    row.add(leftTitleGroup, BorderLayout.CENTER);
                    row.add(btnRead, BorderLayout.EAST);
                    chapterListPanel.add(row);
                    chapterListPanel.add(Box.createVerticalStrut(2));
                }
            }
        }

        //[트랙 B]: 순정 상태(기존 코드 전체 회차 목록 드로잉 트랙)
        else{
            if(txtFiles.isEmpty()){
                JLabel lblNoFile = new JLabel("등록된 회차 파일이 없습니다.", SwingConstants.CENTER);
                lblNoFile.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
                chapterListPanel.add(lblNoFile);
            } else {
                //실시간 양방향 정렬 기준 스위칭 연산 실행
                ArrayList<File> sortedList = new ArrayList<>(txtFiles);
                Collections.sort(sortedList, (f1, f2) -> {
                    try{
                        String num1 = f1.getName().replaceAll("^\\s*(\\d+).*", "$1");
                        String num2 = f2.getName().replaceAll("^\\s*(\\d+).*", "$1");
                        int cmp = Integer.compare(Integer.parseInt(num1), Integer.parseInt(num2));
                        return isSortAscending ? cmp : -cmp;    //변수 플래그에 의한 역순 스위칭
                    } catch(Exception e) {
                        return f1.getName().compareTo(f2.getName());
                    }
                });

                int renderCount = 0;        //실제 화면에 출력된 개수 카운트 변수

                for(File file : sortedList){
                    int chNum = 1;
                    try{
                        String numStr = file.getName().replaceAll("^\\s*(\\d+).*", "$1");
                        chNum = Integer.parseInt(numStr);
                    } catch(Exception e){
                        chNum = 1;
                    }

                    boolean isWithinRage = (startPageRange == 0) || (chNum >= startPageRange && chNum <= endPageRange);

                    //파싱된 회차 번호가 현재 지정된 범위에 포함되는 파일만 화면에 출력
                    if(isWithinRage){
                        renderCount++;
                        String chName = file.getName().replace(".txt", "");

                        //해당 회차에 책갈피 메모가 저장되어 있는지 디스크 실시간 탐색
                        boolean isThisChapterBookmarked = false;
                        File memoFile = new File(currentNovel.getFolderPath() + File.separator + "memo_bookmarks.txt");
                        if(memoFile.exists()){
                            try(BufferedReader br = new BufferedReader(new FileReader(memoFile))){
                                String line;
                                while((line = br.readLine()) != null){
                                    String[] split = line.split("\\|", -1);
                                    if(split.length >= 2 && split[0].trim().equals(String.valueOf(chNum))){
                                        isThisChapterBookmarked = true;
                                        break;
                                    }
                                }
                            } catch (Exception e) { /* 예외 무시 */ }
                        }

                        //회차별 가로 바 패널 생성
                        JPanel row = new JPanel(new BorderLayout());
                        row.setBackground(Color.WHITE);
                        row.setMaximumSize(new Dimension(500, 38));
                        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(242, 244, 246)));

                        //좌측 제목 라벨을 배치할 컨테이너를 생성, 책갈피 뱃지가 참(true)일 때 리스트에 도킹
                        JPanel leftTitleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 4));
                        leftTitleGroup.setBackground(Color.WHITE);

                        JLabel lblChTitle = new JLabel(chName);
                        lblChTitle.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

                        //회차 제목 라벨 클릭 시에도 뷰어 창 열리도록 마우스 인터페이스 이식
                        lblChTitle.setCursor(new Cursor(Cursor.HAND_CURSOR));
                        lblChTitle.setToolTipText("클릭하면 이 회차를 읽기 시작합니다.");

                        final int titleChapterIndex = chNum; //익명 클래스 내부 전달을 위한 final 바인딩

                        lblChTitle.addMouseListener(new java.awt.event.MouseAdapter(){
                            @Override
                            public void mouseClicked(java.awt.event.MouseEvent e){
                                NovelViewerPage viewer = new NovelViewerPage();
                                viewer.openViewer(currentNovel.getFolderPath(), titleChapterIndex, NovelDetailPage.this, currentNovel);
                            }

                        });
                        leftTitleGroup.add(lblChTitle);

                        if(isThisChapterBookmarked){
                            JLabel lblBookmarkBadge = new JLabel();
                            lblBookmarkBadge.setPreferredSize(new Dimension(14, 14));
                            lblBookmarkBadge.setIcon(new Icon() {
                                @Override
                                public void paintIcon(Component c, Graphics g, int x, int y) {
                                    Graphics2D g2 = (Graphics2D) g.create();
                                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                    g2.setColor(new Color(0, 160, 160));        //청록색 단색 마킹

                                    //책갈피 형상 패스 드로잉 연산
                                    int[] px = {x, x+10, x+10, x+5, x};
                                    int[] py = {y, y, y+13, y+9, y+13};
                                    g2.fillPolygon(px, py, 5);
                                    g2.dispose();
                                }

                                @Override
                                public int getIconWidth() {
                                    return 11;
                                }

                                @Override
                                public int getIconHeight() {
                                    return 11;
                                }
                            });
                            leftTitleGroup.add(lblBookmarkBadge);
                        }

                        JButton btnRead = new JButton("보기");
                        btnRead.setFont(new Font("맑은 고딕", Font.BOLD, 12));
                        btnRead.setFocusPainted(false);
                        btnRead.setBorderPainted(false);
                        btnRead.setContentAreaFilled(false);
                        btnRead.setCursor(new Cursor(Cursor.HAND_CURSOR));

                        //글자 색상을 청록색으로 설정
                        btnRead.setForeground(new Color(0, 140, 140));

                        //압축된 행 높이에 맞춰 버튼 내부 마진 여백을 슬림하게 함
                        btnRead.setMargin(new Insets(2, 8, 2, 8));

                        final int chapterIndex = chNum;     //익명 클래스 내부 전달을 위핸 final 변수 처리

                        //보기 버튼을 누르면 해당 소설 폴더 경로를 들고 최종 뷰어를 팝업
                        btnRead.addActionListener(e -> {
                            NovelViewerPage viewer = new NovelViewerPage();
                            viewer.openViewer(currentNovel.getFolderPath(), chapterIndex, this, currentNovel);
                        });

                        row.add(leftTitleGroup, BorderLayout.WEST);
                        row.add(btnRead, BorderLayout.EAST);
                        chapterListPanel.add(row);
                        chapterListPanel.add(Box.createVerticalStrut(2));   //항목 간 간격
                    }
                }

                //예외 처리 : 해당 범위에 파일이 하나도 검출되지 않았을 때의 상태 표기
                if(renderCount == 0){
                    JLabel lblEmptyRange = new JLabel("해당 범위에 포함된 회차 파일이 없습니다.", SwingConstants.CENTER);
                    lblEmptyRange.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                    lblEmptyRange.setForeground(Color.GRAY);
                    chapterListPanel.add(lblEmptyRange);
                }
            }
        }
        chapterListPanel.revalidate();
        chapterListPanel.repaint();
    }

    //해당 소설 폴더 안의 bookmark.txt 사전 추적하는 파일 IO 메서드
    private void loadBookmarkInfo(){
        String bookmarkPath = currentNovel.getFolderPath();
        if(!bookmarkPath.endsWith(File.separator)){
            bookmarkPath += File.separator;
        }
        File file = new File(bookmarkPath + "bookmark.txt");

        if(file.exists()){
            try(BufferedReader br = new BufferedReader(new FileReader(file))){
                String line = br.readLine();
                if(line != null){
                    this.lastReadChapter = Integer.parseInt(line.trim());
                    this.hasBookmark = true;    //읽은 기록 확인 신호 켬
                }
            } catch(IOException | NumberFormatException e){
                this.lastReadChapter = 1;
                this.hasBookmark = false;
            }
        } else{
            this.lastReadChapter = 1;
            this.hasBookmark = false;
        }
    }

    //뷰어가 닫힐 때 NovelViewerPage에서 원격 호출하여 서재 화면을 새로고침하는 명령 인터페이스
    public void triggerShelfRefresh(){
        if(parentShelf != null){
            //1. 변경된 최근 열람일시 데이터와 정렬 순서를 library_data.txt에 영구 백업 지시
            parentShelf.saveLibraryData();

            //2. 복잡한 콤보박스 조작없이 실시간 리프레시 함수를 다이렉트로 관통
            parentShelf.refreshLibrary();
        }
    }

    //회차별 요약 메모 내용을 저장하고 불러오는 팝업 메모장 엔진
    private void openSummaryNoteDialog(){
        //독립형 모달 다이얼로그 개설
        JDialog noteDialog = new JDialog(detailFrame, "회차별 요약 메모 - " + currentNovel.getTitle(), true);
        noteDialog.setSize(400, 450);
        noteDialog.setLocationRelativeTo(detailFrame);
        noteDialog.setLayout(new BorderLayout());

        //안내 문구 배치
        JLabel lblGuide = new JLabel("<html><body>소설의 주요 내용이나 기억해둘 내용을 자유롭게 작성하세요.(ex. 1화 주인공 등장)</body></html>");
        lblGuide.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        lblGuide.setForeground(Color.GRAY);
        lblGuide.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 15));
        noteDialog.add(lblGuide, BorderLayout.NORTH);

        //자유 입력을 위한 텍스트 에어리어 세팅
        JTextArea taSummary = new JTextArea();
        taSummary.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        taSummary.setLineWrap(true);
        taSummary.setWrapStyleWord(true);
        taSummary.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(taSummary);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));
        noteDialog.add(scroll, BorderLayout.CENTER);

        //디스크에서 기존에 저장된 메모 데이터 팡리(summary_notes.txt)역추적 로드
        String targetPath = currentNovel.getFolderPath();
        if(!targetPath.endsWith(File.separator)){
            targetPath += File.separator;
        }
        File noteFile = new File(targetPath + "summary_notes.txt");

        if(noteFile.exists()){
            StringBuilder sb = new StringBuilder();
            try(BufferedReader br = new BufferedReader(new FileReader(noteFile))){
                String line;
                while((line = br.readLine()) != null){
                    sb.append(line).append("\n");
                }
                taSummary.setText(sb.toString());
                //커서를 맨 위로 초기화
                taSummary.setCaretPosition(0);
            } catch(Exception ex){
                System.out.println("요약 메모장 파일 로드 실패: " + ex.getMessage());
            }
        }

        //하단 [저장하기] 제어 단추 조립 패널
        JPanel bottomControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        bottomControlPanel.setBackground(new Color(245, 247, 249));

        JButton btnSave = new JButton("저장하기");
        btnSave.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        btnSave.setBackground(new Color(0, 120, 230));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);

        btnSave.addActionListener(evt -> {
            //덮어쓰기 모드(false)로 파일 스트림을 열어 텍스트 에어리어 내용을 통째로 인쇄
            try(BufferedWriter bw = new BufferedWriter(new FileWriter(noteFile, false))){
                bw.write(taSummary.getText());
                JOptionPane.showMessageDialog(noteDialog, "메모가 정상적으로 디스크에 각인되었습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                noteDialog.dispose();
            } catch(Exception ex){
                JOptionPane.showMessageDialog(noteDialog, "메모 저장 중 오류가 발생했습니다.", "에러", JOptionPane.ERROR_MESSAGE);
            }
        });
        bottomControlPanel.add(btnSave);
        noteDialog.add(bottomControlPanel, BorderLayout.SOUTH);

        //메모장 활성화
        noteDialog.setVisible(true);
    }
}
