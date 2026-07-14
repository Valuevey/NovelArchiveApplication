import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.ArrayList;

public class NovelViewerPage{

    // 현재 열려있는 뷰어 창을 추적하기 위한 정적(static) 변수
    private static JFrame activeViewerFrame = null;
    private static NovelViewerPage activeInstance = null;

    //지역 변수 였던 frame을 멤버 변수로 승격
    private JFrame frame;

    // 기존에 열려있는 뷰어가 있다면 안전하게 종료(책갈피 저장 포함)시키는 메서드
    public static void closeActiveViewer() {
        if (activeViewerFrame != null) {
            // 강제로 창 닫기 이벤트를 발생시켜 windowClosing(책갈피 저장 로직)이 정상 작동하도록 유도
            activeViewerFrame.dispatchEvent(new WindowEvent(activeViewerFrame, WindowEvent.WINDOW_CLOSING));
            activeViewerFrame = null;
        }
    }

    private int currentFontSize = 16; //글자 크기 기억할 변수(기본값 : 16)
    private String currentFontName = "맑은 고딕";  //기본 글꼴 : 맑은 고딕

    //현재 적용 중인 테마 색상을 유지하기 위한 전역 변수
    private Color currentBgColor = Color.WHITE;
    private Color currentFgColor = new Color(33, 33, 33);

    private int currentChapter;
    private int totalChapters = 0;   //폴더 내 .txt. 파일 개수를 동적으로 세어 저장할 변수

    //고정(final) 경로를 해제하고 변수로 변경
    private String folderPath;
    private String bookmarkFile;

    // 현재 읽고 있는 텍스트의 모드(기본:원본)
    private String currentTranslationMode = "원본";

    private JTextPane textArea;  //버튼 누를 때마다 값이 바뀌도록 static 변수로 선언
    private JPanel topPanel;
    private JPanel bottomPanel; //하단에 패널 변수 추가
    private JLabel statusLabel;  //하단에 텍스트를 띄울 레이블
    private JScrollPane scrollPane;
    private JTextField chapterInputField;  //회차 입력창

    // 검색창 관련 멤버 변수
    private JPanel searchBarPanel;
    private JTextField searchField;
    private JLabel searchStatusLabel;
    private java.util.List<Integer> searchResults = new java.util.ArrayList<>();
    private int currentSearchIndex = -1;
    private String currentKeyword = "";

    // 검색어 하이라이트 제어 및 색상 변수
    private java.util.List<Object> highlightTags = new java.util.ArrayList<>();
    private javax.swing.text.DefaultHighlighter.DefaultHighlightPainter normalPainter =
            new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
    private javax.swing.text.DefaultHighlighter.DefaultHighlightPainter currentPainter =
            new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(new Color(255, 150, 0)); // 강조용 주황색

    //나를 호출한 상세 페이지의 주소를 기억할 변수
    private NovelDetailPage detailPageTrigger;

    //메서드가 열릴 때 '폴더 경로'를 함께 매개변수로 받도록 설계
    public void openViewer(String selectedFolderPath, int targetChapter, NovelDetailPage detailPage, Novel novel){
        // 기존에 열린 뷰어가 있다면 책갈피를 저장하고 안전하게 닫음(창 개수 1개 제한)
        closeActiveViewer();

        activeInstance = this;  //현재 새 뷰어를 활성 뷰어로 등록
        this.detailPageTrigger = detailPage;    //호출한 상세창 주소 기억

        //경로 끝에 슬래시가 누락되었다면 붙여주는 처리
        if(!selectedFolderPath.endsWith(File.separator)){
            selectedFolderPath += File.separator;
        }
        this.folderPath = selectedFolderPath;
        this.bookmarkFile = selectedFolderPath + "bookmark.txt";    //해당 소설 폴더 내부에 책갈피 생성

        //사용자가 지정한 폴더 안에 실제 텍스트 파일(.txt)이 몇 개 있는지 자동으로 계산
        countTotalChapters();

        //목록에서 선택해서 들어온 회차 번호가 있다면 그것을 먼저 세팅
        //번호가 0 이하(기본값 신호)일 때만 기존 책갈피 파일을 읽어오도록 분기 처리
        if(targetChapter > 0){
            this.currentChapter = targetChapter;
        } else{
            loadBookmark();
        }

        // 창을 열기 전에 기존에 열려있는 뷰어가 있다면 닫기
        closeActiveViewer();

        //1. 프로그램의 메인 창(JFrame) 생성
        this.frame = new JFrame("모던 웹소설 뷰어");

        //창 닫을 때 바로 종료안하고 책갈피에 저장하도록 설정
        this.frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        this.frame.setSize(750, 730);
        this.frame.setLocationRelativeTo(null);  // 창이 모니터 정 중앙에 뜨도록 설정
        this.frame.setLayout(new BorderLayout());

        //2. 상단 메뉴바(JPanel) 생성
        topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));

        // 이전화/다음화 버튼, 폰트 및 컴포넌트 뼈대 세팅
        JButton btnPrev = new JButton("◀ 이전화");
        JButton btnNext = new JButton("▶ 다음화");

        chapterInputField = new JTextField(String.valueOf(currentChapter), 3);
        chapterInputField.setHorizontalAlignment(JTextField.CENTER);
        JButton btnGo = new JButton("이동");

        // 소설 뷰어 설정 격리용 톱니바퀴 버튼 배치
        JButton btnSettings = new JButton("설정");

        // 번역 버튼 초기화 및 액션
        JButton btnTranslation = new JButton("번역");
        String novelName = new File(folderPath.replaceAll("[\\\\/]$", "")).getName();
        File transRoot = new File("C:\\novel\\translation\\" + novelName);

        //폴더가 이미 존재한다면 즉시 ▼ 모드로 전환
        if(transRoot.exists() && transRoot.listFiles(File::isDirectory) != null && transRoot.listFiles(File::isDirectory).length > 0){
            btnTranslation.setText(currentTranslationMode + "▼");
        }

        btnTranslation.addActionListener(e -> {
            if(btnTranslation.getText().equals("번역")){
                // 폴더 최초 생성 로직
                String input = JOptionPane.showInputDialog(frame, "새 번역본 폴더의 이름을 입력하세요.\n(예: 영어, 일본어 등)");
                if(input != null && !input.trim().isEmpty()){
                    File newTransDir = new File(transRoot, input.trim());
                    if(newTransDir.mkdirs()){
                        currentTranslationMode = "원본";
                        btnTranslation.setText(currentTranslationMode+ " ▼");   //버튼 형태 전환
                        JOptionPane.showMessageDialog(frame, "번역 폴더가 생성되었습니다. 내부에 텍스트 파일을 넣어주세요.\n경로: " + newTransDir.getAbsolutePath());
                    }
                }
            } else{
                // 팝업 메뉴 펼치기
                btnTranslation.setText(currentTranslationMode + " ▲");
                showTranslationPopup(btnTranslation, transRoot, frame);
            }
        });

        JButton btnMemoBookmark = new JButton();
        btnMemoBookmark.setFocusPainted(false);
        btnMemoBookmark.setBorderPainted(false);
        btnMemoBookmark.setContentAreaFilled(false);
        btnMemoBookmark.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMemoBookmark.setToolTipText("현재 화를 메모와 함께 책갈피에 등록합니다.");

        //현재 화의 책갈피 등록 여부를 판별하는 로컬 메서드 즉석 구동
        String checkPath = selectedFolderPath;
        if(!checkPath.endsWith(File.separator)){
            checkPath += File.separator;
        }
        File chkFile = new File(checkPath + "memo_bookmarks.txt");
        boolean isCurrentChBookmarked = false;
        if(chkFile.exists()){
            try(BufferedReader rdr = new BufferedReader(new FileReader(chkFile))){
                String l;
                while((l = rdr.readLine()) != null){
                    String[] sp = l.split("\\|", -1);
                    if(sp.length >= 1 && sp[0].trim().equals(String.valueOf(targetChapter))){
                        isCurrentChBookmarked = true;
                        break;
                    }
                }
            } catch(Exception e){}
        }
        final boolean finalBookmarkState = isCurrentChBookmarked;

        //책갈피 활성화 상태 아이콘 드로잉 엔진
        Icon filledBookmarkIcon = new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 160, 160));    //내부를 가득 채우는 청록색 컬러
                int[] px = {x+3, x+15, x+15, x+9, x+3};
                int[] py = {y+1, y+1, y+18, y+13, y+18};
                g2.fillPolygon(px, py, 5);
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

        //책갈피 비활성화 삼태 아이콘 드로잉 엔진(외곽선만 노출)
        Icon emptyBookmarkIcon = new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(160, 165, 175));
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int[] px = {x+3, x+15, x+15, x+9, x+3};
                int[] py = {y+1, y+1, y+18, y+13, y+18};
                g2.drawPolygon(px, py, 5);
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

        //초기 아이콘 상태 마킹 바인딩
        btnMemoBookmark.setIcon(finalBookmarkState ? filledBookmarkIcon : emptyBookmarkIcon);

        //레이아웃 엔진을 가로 한 줄 강제 고정형인 X_AXIS BoxLayout으로 바꿈
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        //회차 입력창(JTextField)의 테두리를 각진 선 -> 회색 라운드선 스타일로 바꿈
        chapterInputField.setMaximumSize(new Dimension(42, 28));
        chapterInputField.setPreferredSize(new Dimension(42, 28));
        chapterInputField.setFont(new Font("맑은 고딕", Font.BOLD, 12));

        chapterInputField.setForeground(new Color(65, 70, 80));
        chapterInputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 228, 232), 1, true),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));

        //버튼 포커스 테두리 청소
        JButton[] topButton = {btnPrev, btnNext, btnGo, btnTranslation, btnSettings, btnMemoBookmark};
        for(JButton btn: topButton){
            btn.setFocusPainted(false);

            if(btn != btnMemoBookmark){
                //시스템 룩앤필 테두리 덮어쓰기 무력화 지시
                btn.setBorderPainted(false);
                btn.setContentAreaFilled(false);        //배경 투명화
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
                btn.setForeground(new Color(0, 140, 140));

                if(btn == btnGo){
                    btn.setPreferredSize(new Dimension(55, 28));
                    btn.setMinimumSize(new Dimension(55, 28));
                    btn.setMaximumSize(new Dimension(55, 28));
                } else if(btn == btnTranslation) {
                    btn.setPreferredSize(new Dimension(95, 28));
                    btn.setMinimumSize(new Dimension(95, 28));
                    btn.setMaximumSize(new Dimension(95, 28));
                }else{
                    //버튼 크기를 균일하게
                    btn.setPreferredSize(new Dimension(82, 28));
                    btn.setMinimumSize(new Dimension(82, 28));
                    btn.setMaximumSize(new Dimension(82, 28));
                }

                btn.setUI(new javax.swing.plaf.basic.BasicButtonUI(){
                    @Override
                    public void paint(Graphics g, JComponent c){
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        //캡슐 외곽선 수동 드로잉
                        g2.setColor(new Color(220, 225, 230));
                        g2.drawRoundRect(0, 0, c.getWidth()-1, c.getHeight()-1, 8, 8);
                        g2.dispose();
                        super.paint(g, c);
                    }
                });
                btn.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
            }
        }
        btnSettings.setPreferredSize(new Dimension(65, 28));
        btnSettings.setMinimumSize(new Dimension(65, 28));
        btnSettings.setMaximumSize(new Dimension(65, 28));

        topPanel.removeAll();       //기존 배치 찌꺼기 세척 제거

        //상단 바 내비게이션 레이아웃 구성 조립
        topPanel.add(btnPrev);
        topPanel.add(Box.createHorizontalStrut(4));
        topPanel.add(chapterInputField);
        topPanel.add(Box.createHorizontalStrut(4));
        topPanel.add(btnGo);
        topPanel.add(Box.createHorizontalStrut(8));
        topPanel.add(btnNext);

        topPanel.add(Box.createHorizontalGlue());
        topPanel.add(btnTranslation);
        topPanel.add(Box.createHorizontalStrut(6));
        topPanel.add(btnSettings);
        topPanel.add(Box.createHorizontalStrut(6));
        topPanel.add(btnMemoBookmark);

        //책갈피 버튼 클릭 시 메모 입력 팝업 및 파일 세이브 백엔드 엔진
        btnMemoBookmark.addActionListener(evt -> {
            //파일 저장 경로 검증 보정
            String targetPath = folderPath;
            if(!targetPath.endsWith(File.separator)){
                targetPath += File.separator;
            }
            File memoFile = new File(targetPath + "memo_bookmarks.txt");

            //[검증단계]: 현재 회차가 이미 memo_bookmarks.txt에 등록되어 있는지 전수조사
            boolean isAlreadyBookmarked = false;
            ArrayList<String> remainedLines = new ArrayList<>();

            if(memoFile.exists()){
                try(BufferedReader br = new BufferedReader(new FileReader(memoFile))){
                    String line;
                    while((line = br.readLine()) != null){
                        String[] split = line.split("\\|", -1);
                        //라인의 첫 번째 항목(회차)이 현재 열람 중인 회차와 일치하는지 비교
                        if(split.length >= 1 && split[0].trim().equals(String.valueOf(currentChapter))){
                            isAlreadyBookmarked = true; //이미 등록된 회차임을 확인
                        } else{
                            //현재 회차가 아닌 다른 회차의 데이터는 보존 목록에 저장
                            remainedLines.add(line);
                        }
                    }
                } catch(Exception ex){
                    System.out.println("책갈피 읽기 검증 실패 " + ex.getMessage());
                }
            }

            //[분기 A]: 이미 등록된 회차일 경우 -> 책갈피 해제 및 삭제 공정 기동
            if(isAlreadyBookmarked){
                int confirm = JOptionPane.showConfirmDialog(frame,
                        currentChapter + "화에 등록된 책갈피 메모를 삭제하시겠습니까?",
                        "책갈피 메모 삭제", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                if(confirm == JOptionPane.YES_OPTION){
                    //보존 목록(remainedLines) 데이터만 파일에 덮어쓰기 하여 현재 회차 라인을 완전히 삭제
                    try(BufferedWriter bw = new BufferedWriter(new FileWriter(memoFile, false))){
                        for(String remain : remainedLines){
                            bw.write(remain);
                            bw.newLine();
                        }
                        //버튼 아이콘 상태를 빈 책갈피로 즉시 복원 리렌더링
                        btnMemoBookmark.setIcon(emptyBookmarkIcon);
                        JOptionPane.showMessageDialog(frame, currentChapter +
                                "화 책갈피가 정상 해제되었습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                    } catch(Exception ex){
                        System.out.println("책갈피 삭제 반영 쓰기 실패: " + ex.getMessage());
                    }
                }
            }

            //[분기 B]: 미등록 회차일 경우 -> 중복 없이 최초 등록 공정 가동
            else{
                //유저에게 메모 내용을 입력받는 다이얼로그 가동
                String memoInput = JOptionPane.showInputDialog(frame,
                        "이 회차( " + currentChapter + "화 )에 남길 메모를 입력하세요\n(최대 40자 제한, 미입력 시 빈 메모로 저장됩니다.)",
                        "책갈피 메모 등록", JOptionPane.PLAIN_MESSAGE);

                //취소 버튼을 누른 게 아니라면 저장 공정 시작
                if(memoInput != null){
                    String cleanMemo = memoInput.trim().replace("|", " ");
                    if(cleanMemo.length() > 40){
                        cleanMemo = cleanMemo.substring(0, 40) + "..."; //40자 제한 초과 시 자동 말줄임 가드
                    }
                    if(cleanMemo.isEmpty()){
                        cleanMemo = "저장된 메모가 없습니다.";
                    }

                    try(java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.FileWriter(memoFile, true))){
                        bw.write(currentChapter + "|" + cleanMemo);
                        bw.newLine();
                        //저장이 완료되면 즉시 내부가 채워진 청록색 책갈피 아이콘으로 실시간 렌더링을 갱신
                        btnMemoBookmark.setIcon(filledBookmarkIcon);

                        JOptionPane.showMessageDialog(frame, currentChapter + "화에 책갈피 메모가 저장 되었습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                    } catch(Exception ex){
                        System.out.println("책갈피 메모 디스크 쓰기 실패: "+ ex.getMessage());
                    }
                }
            }
        });

        //3. 본문 텍스트 영역 환경 조성
        textArea = new JTextPane();
        textArea.setEditable(false);

        //본문 드래그 방지, 마우스 깜빡이 커서 삭제
        textArea.setFocusable(false);               //뷰어 화면이 마우스 포커스를 가져가지 못하게 차단
        textArea.getCaret().setVisible(false);      //마우스 클릭 시 생기는 입력 커서 숨김 처리

        //모바일 앱 느낌의 좌우 최적 마진 스페이싱
        textArea.setMargin(new Insets(35, 32, 35, 32));

        scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        //4. 하단 진척도 바 세팅
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 6));
        statusLabel = new JLabel();
        statusLabel.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        bottomPanel.add(statusLabel);

        //AppSettings에 동기화된 기본 설정을 뷰어 기동 시 최우선 자동 로드 매핑
        this.currentFontName = AppSettings.getInstance().getDefaultFontName();
        this.currentFontSize = AppSettings.getInstance().getDefaultFontSize();
        updateFont();   //폰트 크기 및 글꼴 뷰어 몬문에 즉시 동적 각인

        String savedTheme = AppSettings.getInstance().getDefaultTheme();
        if(savedTheme.equals("베이지색")){
            this.currentBgColor = new Color(237, 231, 216);
            this.currentFgColor = new Color(60, 52, 44);
        } else if(savedTheme.equals("검정(블랙)")){
            this.currentBgColor = new Color(30, 37, 41);
            this.currentFgColor = new Color(206, 212, 218);
        } else{
            this.currentBgColor = Color.WHITE;
            this.currentFgColor = new Color(33,33,33);
        }

        //5. 초기 데이터 및 화면 렌더링 갱신
        loadChapter(currentChapter);
        changeTheme(currentBgColor, currentFgColor);

        //6. 이전화/다음화 동적 내비게이션 액션 연결
        btnPrev.addActionListener(e -> {
            if(currentChapter > 1){
                currentChapter--;
                loadChapter(currentChapter);
                chapterInputField.setText(String.valueOf(currentChapter));
            } else{
                JOptionPane.showMessageDialog(frame, "첫 화입니다", "안내", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        btnNext.addActionListener(e -> {
            if(currentChapter < totalChapters){
                currentChapter++;
                loadChapter(currentChapter);
                chapterInputField.setText(String.valueOf(currentChapter));
            } else{
                JOptionPane.showMessageDialog(frame, "마지막 화입니다.", "안내", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        btnGo.addActionListener(e -> {
            String inputText = chapterInputField.getText().trim();
            try{
                int inputChapter = Integer.parseInt(inputText);
                if(inputChapter >= 1 && inputChapter <= totalChapters){
                    currentChapter = inputChapter;
                    loadChapter(currentChapter);
                } else{
                    JOptionPane.showMessageDialog(frame, "1화부터" + totalChapters + "화 사이만 이동 가능합니다.", "안내", JOptionPane.INFORMATION_MESSAGE);
                    chapterInputField.setText(String.valueOf(currentChapter));
                }
            } catch(NumberFormatException ex){
                JOptionPane.showMessageDialog(frame, "숫자만 입력 가능합니다.", "에러", JOptionPane.WARNING_MESSAGE);
                chapterInputField.setText(String.valueOf(currentChapter));
            }
        });

        chapterInputField.addActionListener(e -> {
            //엔터 눌러도 이동 가능하게
            btnGo.doClick();
        });

        // 설정 버튼 클릭 시, 글자 크기/글꼴/테마 조절 팝업 모달창을 화면에 띄움
        btnSettings.addActionListener(e -> {
            showSettingDialog(frame);
        });

        //7. 사용자가 창을 닫을 때 실행되는 종료 이벤트
        // 창 종료 시 실시간 날짜 영구 저장 연동 파이프라인
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveBookmark();   //읽던 회차 번호를 파일에 저장

                //창이 닫히는 순간 시스템의 현재 날짜(ex. 2026-06-5)를 측정하여 Novel 데이터에 주입
                if(novel != null){
                    // + 날짜 단위에서 초 단위 타임스탬프로 정밀도를 바꿈
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    novel.setLastReadDate(now.toString());

                    //가짜 우회 코드를 철거하고, 하드디스크 텍스트 파일을 직접 긁어와 최근 열람일을 오늘 날짜로 치환하여 덮어쓰는 영구 동기화 스트림을 배치
                    try{
                        File dataFile = new File("C:\\novel\\library_data.txt");
                        if(dataFile.exists()){
                            java.util.ArrayList<String> lines = new java.util.ArrayList<>();
                            try(BufferedReader br = new BufferedReader(new FileReader(dataFile))){
                                String line;
                                while((line = br.readLine()) != null){
                                    String[] splitData = line.split("\\|", -1);
                                    if(splitData.length >=9 && splitData[0].equals(novel.getTitle())){
                                        splitData[8] = now.toString();
                                        line = String.join("|", splitData);
                                    }
                                    lines.add(line);
                                }
                            }
                            try(BufferedWriter bw = new BufferedWriter(new FileWriter(dataFile))){
                                for(String l : lines){
                                    bw.write(l);
                                    bw.newLine();
                                }
                            }
                        }
                    } catch(Exception ex){
                        System.out.println("원격 실시간 디스크 저장 실패: " + ex.getMessage());
                    }
                }

                //뷰어 창이 완전히 닫히는 순간 상세 정보 창의 버튼 텍스트 새로고침
                if(detailPageTrigger != null){
                    detailPageTrigger.refreshBookmarkStatus();
                    detailPageTrigger.renderChapterList();

                    //데이터의 무결성을 보장하기 위해 메인 보관함의 백업(Save) 후 리프레시를 유도
                    if(detailPageTrigger != null){
                        //1. 상세창이 들고 있는 parentShelf(BookShelfPage) 인스턴스를 원격호출
                        // 현재 NovelDetailPAge.java 내부에는 parentShelf 변수가 확보 되어 있음
                        //NovelDetailPage에 parentShelf를 가져오는 getter 메서드가 없다면
                        //기존에 만들어 둔 triggerShelfRefresh()를 호출하기 전에 저장을 강제 지시

                        //메인 서재 보관함 화면까지 실시간 리프레시 트리거를 원격 발사
                        detailPageTrigger.triggerShelfRefresh();
                    }

                    //뷰어가 닫힐 때 상세 창의 소설 목록 및 책갈피 뱃지를 즉시 리렌더링
                    detailPageTrigger.renderChapterList();
                }

                frame.dispose();  //현재 뷰어 창만 닫고 보관함으로 돌아감
            }
        });

        //9. 전체 레이아웃 조립하기
        JPanel headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.add(topPanel, BorderLayout.NORTH);
        initSearchBar(headerWrapper);   //검색바를 headerWrapper의 하단에 조립

        frame.add(headerWrapper, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // Ctrl+F 검색 단축키 바인딩(JtextPane 포커스 기준)
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

        frame.setVisible(true);
    }

    private void initSearchBar(JPanel parent){
        searchBarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        searchBarPanel.setBackground(new Color(245, 245, 245));
        searchBarPanel.setVisible(false);       //기본은 숨김

        searchField = new JTextField(15);
        searchStatusLabel = new JLabel("0/0");
        searchStatusLabel.setFont(new Font("맑은 고딕", Font.BOLD, 12));

        JButton btnPrevSearch = new JButton("▲");
        JButton btnNextSearch = new JButton("▼");
        JButton btnCloseSearch = new JButton("X");

        JButton[] sBtns = {btnPrevSearch, btnNextSearch, btnCloseSearch};
        for(JButton b : sBtns){
            b.setFont(new Font("맑은 고딕", Font.BOLD, 12));
            b.setFocusPainted(false);
            b.setContentAreaFilled(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

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
                navigateSearch(1);
            } else{
                performSearch(keyword);
            }
        });

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

        parent.add(searchBarPanel, BorderLayout.SOUTH);
    }

    private void performSearch(String keyword){
        if(textArea == null) return;

        javax.swing.text.Highlighter h = textArea.getHighlighter();
        h.removeAllHighlights();
        searchResults.clear();
        highlightTags.clear();
        currentSearchIndex = -1;
        currentKeyword = keyword;

        if(keyword.trim().isEmpty()){
            searchStatusLabel.setText("0/0");
            return;
        }

        try{
            // ※ JTextPane의 HTML 태그를 무시하고 순수 텍스트만 가져오기 위한 안전 코드
            javax.swing.text.Document doc = textArea.getDocument();
            String content = doc.getText(0, doc.getLength());
            int index = content.indexOf(keyword);

            while(index >= 0){
                Object tag = h.addHighlight(index, index + keyword.length(), normalPainter);
                highlightTags.add(tag);
                searchResults.add(index);
                index = content.indexOf(keyword, index + keyword.length());
            }
        } catch(Exception ex){
            ex.printStackTrace();
        }

        if(!searchResults.isEmpty()){
            navigateSearch(1);
        } else{
            searchStatusLabel.setText("0/0");
            JOptionPane.showMessageDialog(frame, "일치하는 단어를 찾을 수 없습니다.");
        }
    }

    private void navigateSearch(int direction){
        if(searchResults.isEmpty()) return;

        javax.swing.text.Highlighter h = textArea.getHighlighter();

        if(currentSearchIndex >= 0 && currentSearchIndex < highlightTags.size()){
            try{
                h.removeHighlight(highlightTags.get(currentSearchIndex));
                int oldPos = searchResults.get(currentSearchIndex);
                Object normalTag = h.addHighlight(oldPos, oldPos + currentKeyword.length(), normalPainter);
                highlightTags.set(currentSearchIndex, normalTag);
            } catch(Exception e){}
        }

        if(currentSearchIndex == -1){
            currentSearchIndex = 0;
        } else{
            currentSearchIndex = (currentSearchIndex + direction + searchResults.size()) % searchResults.size();
        }

        int pos = searchResults.get(currentSearchIndex);
        try{
            h.removeHighlight(highlightTags.get(currentSearchIndex));
            Object currentTag = h.addHighlight(pos, pos + currentKeyword.length(), currentPainter);
            highlightTags.set(currentSearchIndex, currentTag);

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

    //상단바 잘림을 해결하는 글자/테마 다용도 가독성 설정 모달 팝업
    private void showSettingDialog(JFrame parent){
        JDialog dialog = new JDialog(parent, "뷰어 설정", true);
        dialog.setSize(440, 350);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());

        //취소 기능을 위해 다이얼로그 오픈 시점의 초기 설정 스냅샷을 백업
        final int backupSize = currentFontSize;
        final String backupFont = currentFontName;
        final Color backupBg = currentBgColor;
        final Color backupFg = currentFgColor;

        Color themeCyan = new Color(0, 140, 140);
        Color borderGray = new Color(215, 222, 228);
        Color lineCyan = new Color(200, 225, 225);  //연한 청록색 점선용 컬러

        //메인 콘텐츠 컨테이너 패널 세팅
        JPanel mainContentPanel = new JPanel(){
            @Override
            protected void paintComponent(Graphics g){
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                //연한 청록색 점선 스타일
                g2.setColor(lineCyan);
                float[] dash = {4.0f, 4.0f};
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dash, 0.0f));

                //각 행 사이에 점선 배치
                g2.drawLine(25, 62, getWidth()-25, 62);
                g2.drawLine(25, 134, getWidth()-25, 134);
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
        lblSizeTitle.setForeground(new Color(50, 55, 60));
        lblSizeTitle.setPreferredSize(new Dimension(140, 40));
        sizeRow.add(lblSizeTitle);

        //마이너스 조절 버튼 생성(둥근 모서리 디자인)
        JButton btnMinus = new JButton();
        btnMinus.setPreferredSize(new Dimension(36, 32));
        btnMinus.setFocusPainted(false);
        btnMinus.setBorderPainted(false);
        btnMinus.setContentAreaFilled(false);
        btnMinus.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMinus.setUI(new javax.swing.plaf.basic.BasicButtonUI(){
            @Override
            public void paint(Graphics g, JComponent c){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(borderGray);
                g2.drawRoundRect(0, 0, c.getWidth()-1, c.getHeight()-1, 6, 6);

                g2.setColor(themeCyan);
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(13, 16, 23, 16);    //마이너스 기호 수동 정밀 조준 드로잉
                g2.dispose();
            }
        });

        //중앙 숫자 입력 텍스트 필드 생성
        JTextField tfSizeInput = new JTextField(String.valueOf(currentFontSize), 3);
        tfSizeInput.setHorizontalAlignment(JTextField.CENTER);
        tfSizeInput.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
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
        btnPlus.setUI(new javax.swing.plaf.basic.BasicButtonUI(){
            @Override
            public void paint(Graphics g, JComponent c){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(borderGray);
                g2.drawRoundRect(0, 0, c.getWidth()-1, c.getHeight()-1, 6, 6);

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
        lblFontTitle.setForeground(new Color(50, 55, 60));
        lblFontTitle.setPreferredSize(new Dimension(140, 45));
        fontRow.add(lblFontTitle);

        String[] fonts = {"맑은 고딕", "나눔고딕", "바탕체", "돋움", "굴림"};
        JComboBox<String> fontComboBox = new JComboBox<>(fonts);
        fontComboBox.setSelectedItem(currentFontName);  //현재 지정 중인 폰트명이 자동 포커싱되도록 유도
        fontComboBox.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
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
        lblThemeTitle.setForeground(new Color(50, 55, 60));
        lblThemeTitle.setPreferredSize(new Dimension(140, 45));
        themeRow.add(lblThemeTitle);

        JButton btnThemeWhite = new JButton("흰색");
        JButton btnThemeSepia = new JButton("베이지");
        JButton btnThemeDark = new JButton("검정");

        JButton[] themeButtons = {btnThemeWhite, btnThemeSepia, btnThemeDark};
        for(JButton btn : themeButtons){
            btn.setPreferredSize(new Dimension(70, 32));
            btn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }



        //각 테마 버튼에 고유한 배경/글자색 주입, 선택시 청록색 테두리
        java.util.function.Consumer<String> refreshThemeButtonStyles = (activeTheme) -> {
            // 1. 선택된 활성화 상태에 따른 글자(Foreground) 색상 동적 매핑
            btnThemeWhite.setForeground(activeTheme.equals("흰색") ? new Color(33, 33, 33) : new Color(140, 145, 155));
            btnThemeSepia.setForeground(activeTheme.equals("베이지") ? new Color(60, 52, 44) : new Color(120, 125, 135));
            btnThemeDark.setForeground(activeTheme.equals("검정") ? Color.WHITE : new Color(140, 145, 155));

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

                    g.setColor(activeTheme.equals("검정") ? Color.WHITE : new Color(140, 145, 155));
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
            btn.setFont(new Font("맑은 고딕", Font.BOLD, 13));
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

        // ----------실시간 이벤트 연동 처리 리스너 매핑----------
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

        //[우측 상단 X 버튼 종료 대응]: 취소 버튼과 완전히 동일
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                currentFontSize = backupSize;
                currentFontName = backupFont;
                currentBgColor = backupBg;
                currentFgColor = backupFg;
                updateFont();
                changeTheme(currentBgColor, currentFgColor);
                dialog.dispose();
            }
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

    //폴더 내의 txt 파일 개수를 동적으로 카운트하는 메서드
    private void countTotalChapters(){
        File dir = new File(folderPath);
        File[] files = dir.listFiles((dir1, name) ->
                name.toLowerCase().endsWith(".txt") && !name.equals("bookmark.txt") && !name.equals("memo_bookmarks.txt"));
        if(files != null){
            this.totalChapters = files.length;
        }
        if(this.totalChapters == 0){
            this.totalChapters = 1; //최소 기본값 예외 처리
        }
    }

    //지정된 회차 파일을 읽어와 화면과 하단 상태바를 갱신하는 메서드
    private void loadChapter(int chapterNumber){
        StringBuilder sb = new StringBuilder();
        File targetFile = null;

        // 1. 읽어야 할 폴더 경로 결정(원본 vs 번역본)
        String searchPath = folderPath;
        if(!"원본".equals(currentTranslationMode)){
            File novelFolder = new File(folderPath.replaceAll("[\\\\/]$", ""));
            // c:\novel\translation\[작품명]\[언어] 폴더로 경로 변경
            searchPath = "C:\\novel\\translation\\" + novelFolder.getName() + File.separator + currentTranslationMode + File.separator;
        }

        //폴더 안의 모든 파일을 전수 조사하여 맨 앞 숫자가 타겟 회차 번호와 일치하는 실제 파일을 발굴
        File dir = new File(searchPath);

        if(dir.exists() && dir.isDirectory()){
            File[] files = dir.listFiles((dir1, name) ->
                    name.toLowerCase().endsWith(".txt") && !name.equals("bookmark.txt") && !name.equals("memo_bookmarks.txt"));
            if(files != null){
                for(File f : files){
                    try{
                        //파일명 앞자리 숫자 추출 시도
                        String origName = f.getName();
                        String numStr = origName.replaceAll("^\\s*(\\d+).*", "$1");

                        //파일명이 숫자로 시작하는 장편 소설 기조일 때
                        if(numStr.matches("\\d+")){
                            if(Integer.parseInt(numStr) == chapterNumber){
                                targetFile = f;
                                break;
                            }
                        }
                        // [단편/썰 대응 안전코드] 숫자가 없는 순전 문장형 파일명일 때
                        else if(folderPath.contains("2차텍스트")){
                            //단편 서재는 파일 하나가 곧 전체이므로, 폴더 내부의 첫 번째 유효 텍스트 파일을 직접 타겟팅
                            targetFile = f;
                            break;
                        }
                    } catch(Exception ex) {}
                }
            }
        }

        //일치하는 매칭 파일을 찾지 못했을 경우의 방어 예외 처리
        if(targetFile == null || !targetFile.exists()){
            textArea.setContentType("text/plain");  //에러 출력 모드 안정화
            textArea.setText("회차 파일을 찾을 수 없습니다.\n타겟 회차 번호: " + chapterNumber + "화\n폴터 내 파일명을 확인하세요.");
            return;
        }

        //글꼴 유실 방지를 위해 UTF-8 인코딩 스트림 명시적 선언 명명
        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(targetFile), "UTF-8"))){
            textArea.setContentType("text/html");

            String firstLine = br.readLine();   //파일의 첫 번째 줄(제목)을 따로 떼어냄
            StringBuilder bodySb = new StringBuilder();
            String line;
            while((line = br.readLine()) != null){
                bodySb.append(line).append("<br>"); //html 줄바꿈 태그로 치환
            }

            // 삽화 삽입 로직 시작
            // 텍스트 폴더 경로(folderPath)에서 작품 폴더 이름(Novel Title)을 추출
            File txtFolder = new File(folderPath.replaceAll("[\\\\/]$", ""));
            String actualFolderName = txtFolder.getName();      // 실제 폴더명 사용

            // ImageManagerDialog에서 생성한 폴더명과 동일하게 특수문자 정제
            String safeNovelName = folderPath.replaceAll("[\\\\/:*?\"<>|]", "_");

            // 이미지 폴더 경로: C:\novel\images\[작품명]\mapping.txt
            String mappingPath = "C:\\novel\\images\\" + actualFolderName + "\\mapping.txt";
            File mappingFile = new File(mappingPath);

            if(mappingFile.exists()){
                try(BufferedReader mapBr = new BufferedReader(new InputStreamReader(new FileInputStream(mappingFile), "UTF-8"))){
                    String mapLine;
                    while((mapLine = mapBr.readLine()) != null){
                        String[] parts = mapLine.split("=", 2);
                        if(parts.length < 2) continue;

                        String fileName = parts[0];
                        String[] meta = parts[1].split("\\|");  // 제목|회차|설명

                        // 2. 현재 회차와 매핑된 회차가 일치하거나 ALL인 경우 삽입
                        if(meta.length >= 2 && meta[1].equals(String.valueOf(chapterNumber))){
                            // 이미지 소스 경로: C:\novel\images\[작품명]\[파일명]
                            String imageSrc = "C:\\novel\\images\\" + actualFolderName + File.separator + fileName;

                            // HTML 하단에 이미지 추가
                            bodySb.append("<br><br><div style='text-align:center;'>");
                            bodySb.append("<img src='file:///" + imageSrc.replace("\\", "/") + "' width='400'>");
                            bodySb.append("<br><p style='color:gray; font-size:10pt;'>" + (meta.length > 2 ? meta[2] : "") + "</p>");
                            bodySb.append("</div>");
                        }
                    }
                } catch (IOException e) { e.printStackTrace(); }
            }

            String titleText = (firstLine != null) ? firstLine.trim() : "";

            String htmlTitleStyle = "text-align: center; font-family: " + currentFontName
                    + "; font-size: 15pt; font-weight: bold; color: rgb("
                    + currentFgColor.getRed() + ", " + currentFgColor.getGreen() + ", " + currentFgColor.getBlue()
                    + "); margin-bottom: 8px;";
            String htmlDecorStyle = "text-align: center; color: rgb(200, 210, 215); font-size: 10pt; margin-bottom: 25px;";

            String htmlBodyStyle = "font-family: " + currentFontName +
                    "; font-size: " + currentFontSize + "pt; " +
                    "color: rgb(" + currentFgColor.getRed() + ", " + currentFgColor.getGreen() + ", " + currentFgColor.getBlue() + ");";

            String decorLine = "─────── ◆ ───────";

            String finalHtml = "<html><body style='padding: 0; margin: 0;'>"
                    + "<div style='" + htmlTitleStyle + "'>" + titleText + "</div>"
                    + "<div style='" + htmlDecorStyle + "'>" + decorLine + "</div>"
                    + "<div style='" + htmlBodyStyle + "'>" + bodySb.toString() + "</div>"
                    + "</body></html>";
            textArea.setText(finalHtml);

            // 단편 서재일 때는 하단 진행률을 단편 전용 포맷으로 가독성 처리
            if (folderPath.contains("2차텍스트")) {
                statusLabel.setText("[ 단편 원고 열람 중 ] - " + targetFile.getName().replace(".txt", ""));
            } else {
                int progress = (int) (((double) chapterNumber / totalChapters) * 100);
                statusLabel.setText("[ " + chapterNumber + "화 / 총 " + totalChapters + "화 ] (진행률 " + progress + "%)");
            }

            textArea.setCaretPosition(0);
        } catch (IOException e){
            textArea.setContentType("text/plain");      //에러 텍스트 표출을 위한 일반 텍스트 모드 복원
            textArea.setText("회차 파일을 불러오는 중 오류가 발생했습니다.\n경로를 확인하세요: " + targetFile);
        }
    }

    //현재 읽고 있는 회차 번호를 bookmark.txt 파일에 쓰는 메서드
    //현재 읽고 있는 회차 번호를 bookmark.txt 파일에 쓰는 메서드
    private void saveBookmark(){
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(bookmarkFile))){
            bw.write(String.valueOf(currentChapter));
        } catch(IOException e){
            System.out.println("책갈피 저장 실패: " + e.getMessage());
        }
    }

    //프로그램 싲가 시 bookmark.txt. 파일에서 회차 번호를 읽어오는 메서드
    private void loadBookmark(){
        File file = new File(bookmarkFile);

        //저장된 책갈피 파일이 존재하는 경우에만 읽기
        if(file.exists()){
            try(BufferedReader br = new BufferedReader(new FileReader(file))){
                String line = br.readLine();
                if(line != null){
                    currentChapter = Integer.parseInt(line.trim());
                }
            } catch (IOException | NumberFormatException e){
                currentChapter = 1;   //오류 발생 시 기본값인 1화로 설정
            }
        } else{
            currentChapter = 1;   //파일이 없으면 처음이므로 1화로 설정
        }
    }

    //글자 크기가 바뀔때마다 실행되어 화면의 글꼴을 새로 고침 해주는 메서드
    private void updateFont(){
        if(textArea == null) return;

        // 현재 화차를 현재 설정값으로 다시 렌더링
        int savedPosition = textArea.getCaretPosition(); // 스크롤 위치 보존
        loadChapter(currentChapter);

        // 스크롤 위치 복원 시도
        try{
            textArea.setCaretPosition(savedPosition);
        } catch(Exception e){
            textArea.setCaretPosition(0);
        }
    }

    //호출 시 배경색과 글자색을 받아 모든 컴포넌트의 테마를 일괄 변경하는 메서드
    private void changeTheme(Color backgroundColor, Color foregroundColor){
        if(textArea == null) return;

        //본문 영역 변경
        textArea.setBackground(backgroundColor);
        textArea.setForeground(foregroundColor);

        //상단 바 패널 변경
        topPanel.setBackground(backgroundColor);
        //하단 영역 테마 연동
        bottomPanel.setBackground(backgroundColor);
        statusLabel.setForeground(foregroundColor);
        //스크롤 패널 밴경 연동(스크롤바 여백 공간 처리)
        scrollPane.getViewport().setBackground(backgroundColor);

        // HTML을 문자열 치환 대신 loadChapter로 다시 그림 (HTML 모드 유지)
        loadChapter(currentChapter);
    }

    // 번역본 드롭다운 팝업 패널을 조립하고 화면에 띄우는 메서드
    private void showTranslationPopup(JButton btnTrans, File transRoot, JFrame parentFrame){
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(Color.WHITE);
        popup.setBorder(BorderFactory.createLineBorder(new Color(215, 222, 228), 1));

        // 커스텀 구분선 생성 엔진
        java.util.function.Supplier<JPopupMenu.Separator> createSeparator = () -> new JPopupMenu.Separator(){
            @Override
            public void paintComponent(Graphics g){
                g.setColor(new Color(230, 235, 240));
                g.drawLine(10, getHeight() / 2, getWidth() - 10, getHeight() / 2);
            }
            @Override
            public Dimension getPreferredSize() { return new Dimension(0, 5); }
        };

        // 메뉴 아이템 팩토리
        java.util.function.Function<String, JMenuItem> createMenuItem = (text) -> {
            JMenuItem item = new JMenuItem(text);
            item.setBackground(Color.WHITE);
            item.setFont(new Font("맑은 고딕", Font.BOLD, 12));
            item.setForeground(new Color(50, 55, 60));
            item.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            item.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return item;
        };

        // 1. [+ 추가] 버튼
        JMenuItem addMenu = createMenuItem.apply("+ 추가");
        addMenu.setForeground(new Color(0, 140, 140));
        addMenu.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(parentFrame, "추가할 번역본 폴더의 이름을 입력하세요.\n(예: 영어, 일본어 등)");
            if(input != null && !input.trim().isEmpty()){
                new File(transRoot, input.trim()).mkdirs();
                // 생성 후 즉시 팝업이 닫히며 원상복구 됨
            }
        });
        popup.add(addMenu);
        popup.add(createSeparator.get());

        // 2. [원본] 버튼(원래 소설로 되돌아가기 위함)
        JMenuItem origMenu = createMenuItem.apply("원본");
        origMenu.addActionListener(e -> {
            currentTranslationMode = "원본";
            loadChapter(currentChapter);
            btnTrans.setText("원본 ▼");
        });
        popup.add(origMenu);

        // 3. 만들어진 번역본 폴더들을 스캔하여 목록에 동적 추가
        File[] transDirs = transRoot.listFiles(File::isDirectory);
        if(transDirs != null){
            for(File d : transDirs){
                popup.add(createSeparator.get());
                JMenuItem tMenu = createMenuItem.apply(d.getName());
                tMenu.addActionListener(e -> {
                    currentTranslationMode = d.getName();
                    loadChapter(currentChapter); // 화면 갱신 추가
                    btnTrans.setText(currentTranslationMode + " ▼"); // 텍스트 업데이트
                });
                popup.add(tMenu);
            }
        }

        // 팝업이 닫일 때 버튼을 세모에서 다시 세모 뒤집은 거로 원상 복구
        popup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                btnTrans.setText(currentTranslationMode + " ▼");
            }
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });

        // 버튼 바로 아래에 팝업 출력
        popup.show(btnTrans, 0, btnTrans.getHeight());
    }
}