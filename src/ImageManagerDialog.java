import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class ImageManagerDialog extends JDialog {
    private String imageFolder;
    private JPanel galleryPanel;
    private String novelTitle;
    private String textFolderPath;  //실제 텍스트 폴더 경로를 저장할 변수

    public ImageManagerDialog(JFrame parent, String novelTitle, String textFolderPath){
        super(parent, "삽화 관리 - " + novelTitle, true);
        this.novelTitle = novelTitle;
        this.textFolderPath = textFolderPath;   //경로 저장

        // 경로를 안전하게 조합
        String baseDir = "C:\\novel\\images";
        String folderName = getSafeFolderName(novelTitle);
        this.imageFolder = baseDir + File.separator + folderName;

        // 폴더가 없으면 여기서 확실하게 생성
        File dir = new File(this.imageFolder);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        setSize(450, 600);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);    //전체 배경 흰색

        galleryPanel = new JPanel();
        galleryPanel.setLayout(new BoxLayout(galleryPanel, BoxLayout.Y_AXIS));
        galleryPanel.setBackground(Color.WHITE);
        refreshGallery();

        JScrollPane scrollPane = new JScrollPane(galleryPanel);
        scrollPane.setBorder(null);     //외곽선 제거
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        JButton btnAdd = new JButton("삽화/이미지 추가");
        btnAdd.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        btnAdd.setBackground(new Color(0, 140, 140));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);
        btnAdd.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 폼 다이얼로그 호출
        btnAdd.addActionListener(e -> openAddDialog());
        add(btnAdd, BorderLayout.SOUTH);
    }

    private void refreshGallery(){
        galleryPanel.removeAll();
        File folder = new File("C:\\novel\\images\\" + getSafeFolderName(novelTitle));
        File[] files = folder.listFiles((d, name) -> name.endsWith(".png") || name.endsWith(".jpg"));
        Map<String, String> mapping = loadMapping();

        if(files != null){
            for(File file : files){
                if(!mapping.containsKey(file.getName())) continue;

                String meta = mapping.get(file.getName());
                String[] rawParts = meta.split("\\|");

                // 데이터 파편화로 인해 배열 길이가 짧을 경우를 대비한 안전 장치
                String[] parts = new String[]{
                        rawParts.length > 0 ? rawParts[0] : "제목 없음",
                        rawParts.length > 1 ? rawParts[1] : "ALL",
                        rawParts.length > 2 ? rawParts[2] : "설명 없음",
                };

                JPanel row = createRowPanel(file, parts);
                galleryPanel.add(row);
            }
        }
        galleryPanel.revalidate();
        galleryPanel.repaint();
    }

    // 목록형 UI 조립기
    private JPanel createRowPanel(File file, String[] meta){
        JPanel row = new JPanel(new BorderLayout(10, 5));
        row.setBackground(Color.WHITE);

        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(235, 235, 235)),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        // BoxLayout의 세로 팽창 방지용 락
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 좌측: 제목과 회차를 묶는 텍스트 패널
        JPanel textGroup = new JPanel();
        textGroup.setLayout(new BoxLayout(textGroup, BoxLayout.Y_AXIS));
        textGroup.setBackground(Color.WHITE);

        // 제목
        JLabel lblTitle = new JLabel(meta[0]);
        lblTitle.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        lblTitle.setForeground(new Color(30, 30, 30));

        // 회차 설정
        JLabel lblChapter = new JLabel("[" + meta[1] + "편설정]");
        lblChapter.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        lblChapter.setForeground(new Color(150, 150, 150));

        textGroup.add(lblTitle);
        textGroup.add(Box.createVerticalStrut(5));
        textGroup.add(lblChapter);

        // 우측: ︙ 옵션 버튼
        JLabel lblOpt = new JLabel("︙", SwingConstants.CENTER);
        lblOpt.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        lblOpt.setForeground(new Color(150, 155, 165));
        lblOpt.setPreferredSize(new Dimension(30, 30));
        lblOpt.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // ︙ 버튼 클릭 시 팝업 메뉴 출력
        lblOpt.addMouseListener(new java.awt.event.MouseAdapter(){
            public void mousePressed(java.awt.event.MouseEvent e) {
                e.consume(); // 행 전체 클릭 이벤트 방지
                JPopupMenu popup = new JPopupMenu();
                JMenuItem menuEdit = new JMenuItem("수정");
                JMenuItem menuDel = new JMenuItem("삭제");

                // 하단에 추가될 새 메서드들과 연결
                menuEdit.addActionListener(evt -> openEditDialog(file, meta));
                menuDel.addActionListener(evt -> deleteImage(file));

                popup.add(menuEdit);
                popup.add(menuDel);
                popup.show(lblOpt, e.getX(), e.getY());
            }
        });

        row.add(textGroup, BorderLayout.CENTER);
        row.add(lblOpt, BorderLayout.EAST);

        // 클릭 시 뷰어 오픈 및 마우스 호버 효과
        row.addMouseListener(new java.awt.event.MouseAdapter(){
            public void mouseClicked(java.awt.event.MouseEvent e){
                openImageViewer(file, meta);
            }
            public void mouseEntered(java.awt.event.MouseEvent e){
                row.setBackground(new Color(248, 249, 250));
                textGroup.setBackground(new Color(248, 249, 250));
            }
            public void mouseExited(java.awt.event.MouseEvent e){
                row.setBackground(Color.WHITE);
                textGroup.setBackground(Color.WHITE);
            }
        });
        return row;
    }

    // 일괄 등록 폼(Form) 엔진
    private void openAddDialog() {
        JDialog addDialog = new JDialog(this, "새 삽화 등록", true);
        addDialog.setSize(400, 480);
        addDialog.setLocationRelativeTo(this);
        addDialog.setLayout(new BorderLayout());
        addDialog.getContentPane().setBackground(Color.WHITE);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 10, 0);
        gbc.weightx = 1.0;

        JTextField txtTitle = new JTextField();
        JTextArea txtDesc = new JTextArea(4, 20);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(txtDesc);

        JTextField txtChapter = new JTextField("기본", 10);
        txtChapter.setPreferredSize(new Dimension(80, 28));
        txtChapter.setMinimumSize(new Dimension(80, 28));
        txtChapter.setMaximumSize(new Dimension(80, 28));

        JButton btnSelectChapter = new JButton("회차 선택 ▼");
        btnSelectChapter.setBackground(Color.WHITE);
        btnSelectChapter.setFocusPainted(false);
        btnSelectChapter.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 동적으로 전체 화수 계산
        int dynamicTotalChapters = getTotalChapters();

        btnSelectChapter.addActionListener(e -> {
            JPopupMenu chapterPopup = new JPopupMenu();

            // 팝업 내부에 들어갈 세로 정렬 패널
            JPanel popupPanel = new JPanel();
            popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
            popupPanel.setBackground(Color.WHITE);

            // '기본' 항목 추가
            JButton defaultBtn = new JButton("기본");
            defaultBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            defaultBtn.setBackground(Color.WHITE);
            defaultBtn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
            defaultBtn.addActionListener(evt -> {
                txtChapter.setText("기본");
                chapterPopup.setVisible(false);
            });
            popupPanel.add(defaultBtn);

            // 1화부터 dynamicTotalChapters까지 메뉴 생성
            for (int i = 1; i <= dynamicTotalChapters; i++) {
                String ch = String.valueOf(i);
                JButton btn = new JButton(ch + "화");
                btn.setAlignmentX(Component.CENTER_ALIGNMENT);
                btn.setBackground(Color.WHITE);
                btn.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
                btn.addActionListener(evt -> {
                    txtChapter.setText(ch);
                    chapterPopup.setVisible(false);
                });
                popupPanel.add(btn);
            }
            // 패널을 스크롤팬에 넣어서 팝업에 부착 (스크롤 기능 활성화)
            JScrollPane scrollPane = new JScrollPane(popupPanel);
            scrollPane.setPreferredSize(new Dimension(120, 200));
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16); // 휠 속도 보정

            chapterPopup.add(scrollPane);
            chapterPopup.show(btnSelectChapter, 0, btnSelectChapter.getHeight());
        });

        // 텍스트필드와 버튼을 하나로 묶는 가로형 패널
        JPanel chapterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chapterPanel.setBackground(Color.WHITE);
        chapterPanel.add(txtChapter);
        chapterPanel.add(Box.createHorizontalStrut(5));
        chapterPanel.add(btnSelectChapter);

        JLabel lblFile = new JLabel("선택된 파일: 없음");
        lblFile.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        lblFile.setForeground(Color.GRAY);

        JButton btnSelectFile = new JButton("이미지 파일 찾기");
        final File[] selectedFile = {null};
        btnSelectFile.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(imageFolder);
            if(chooser.showOpenDialog(addDialog) == JFileChooser.APPROVE_OPTION) {
                selectedFile[0] = chooser.getSelectedFile();
                lblFile.setText("선택된 파일: " + selectedFile[0].getName());
            }
        });


        // UI 폼 조립
        gbc.gridy = 0; formPanel.add(new JLabel("제목:"), gbc);
        gbc.gridy = 1; formPanel.add(txtTitle, gbc);
        gbc.gridy = 2; formPanel.add(new JLabel("회차 (예: 3 또는 기본):"), gbc);
        gbc.gridy = 3; formPanel.add(chapterPanel, gbc); // 묶어둔 패널로 교체
        gbc.gridy = 4; formPanel.add(new JLabel("설명:"), gbc);
        gbc.gridy = 5; formPanel.add(descScroll, gbc);
        gbc.gridy = 6; formPanel.add(btnSelectFile, gbc);
        gbc.gridy = 7; formPanel.add(lblFile, gbc);

        addDialog.add(formPanel, BorderLayout.CENTER);

        JButton btnSave = new JButton("등록 완료");
        btnSave.setBackground(new Color(0, 140, 140));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        btnSave.addActionListener(e -> {
            if(selectedFile[0] == null){
                JOptionPane.showMessageDialog(addDialog, "이미지 파일을 선택해주세요.");
                return;
            }
            if(txtTitle.getText().trim().isEmpty()){
                JOptionPane.showMessageDialog(addDialog, "제목을 입력해주세요.");
                return;
            }

            File dest = new File(imageFolder, selectedFile[0].getName());
            try {
                Files.copy(selectedFile[0].toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // 파이프라인(|) 기호 오염 방지 및 줄바꿈 태그 치환 처리
                String cleanTitle = txtTitle.getText().replace("|", " ");
                String cleanChapter = txtChapter.getText().replace("|", " ");
                String cleanDesc = txtDesc.getText().replace("|", " ").replace("\n", "<br>");

                saveMapping(selectedFile[0].getName(), cleanTitle + "|" + cleanChapter + "|" + cleanDesc);
                refreshGallery();
                addDialog.dispose();
            } catch (IOException ex){
                JOptionPane.showMessageDialog(addDialog, "파일 저장 중 오류가 발생했습니다.");
            }
        });

        addDialog.add(btnSave, BorderLayout.SOUTH);
        addDialog.setVisible(true);
    }

    private void saveMapping(String fileName, String meta) {
        Map<String, String> map = loadMapping();
        map.put(fileName, meta);
        String safePath = "C:\\novel\\images\\" + getSafeFolderName(novelTitle);
        new File(safePath).mkdirs(); // 폴더가 없을 경우 대비
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(safePath + "\\mapping.txt"), "UTF-8"))) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                pw.println(entry.getKey() + "=" + entry.getValue());
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private Map<String, String> loadMapping() {
        Map<String, String> map = new HashMap<>();
        File mapFile = new File(imageFolder + "\\mapping.txt");
        if (mapFile.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(mapFile), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) map.put(parts[0], parts[1]);
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
        return map;
    }

    private void openImageViewer(File file, String[] meta) {
        JDialog viewer = new JDialog(this, meta[0], true);
        viewer.setSize(600, 700);
        viewer.setLocationRelativeTo(this);
        viewer.getContentPane().setBackground(Color.WHITE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel titleWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titleWrapper.setBackground(Color.WHITE);
        JLabel titleLbl = new JLabel(meta[0]);
        titleLbl.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        titleWrapper.add(titleLbl);
        panel.add(titleWrapper);

        panel.add(Box.createVerticalStrut(10));

        // 구분선
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(460, 1));
        panel.add(sep);

        panel.add(Box.createVerticalStrut(20));

        // 화면 밖으로 넘치지 않게 고화질 이미지 리사이징 보정
        JPanel imgWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        imgWrapper.setBackground(Color.WHITE);
        try {
            Image originImage = javax.imageio.ImageIO.read(file);
            int imgWidth = originImage.getWidth(null);
            int imgHeight = originImage.getHeight(null);

            if(imgWidth > 420){
                double scale = 420.0 / imgWidth;
                imgWidth = 420;
                imgHeight = (int)(imgHeight * scale);
            }

            Image scaledImage = originImage.getScaledInstance(imgWidth, imgHeight, Image.SCALE_SMOOTH);
            imgWrapper.add(new JLabel(new ImageIcon(scaledImage)));
        } catch(IOException e){
            imgWrapper.add(new JLabel("이미지를 불러올 수 없습니다."));
        }
        panel.add(imgWrapper);

        panel.add(Box.createVerticalStrut(20));

        
        // 줄바꿈(<br>)이 반영된 HTML 구조 설명 출력
        JPanel descWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        descWrapper.setBackground(Color.WHITE);
        String description = (meta.length > 2) ? meta[2] : "설명 없음";
        JLabel descLbl = new JLabel("<html><div style='width: 420px; text-align: left; color: #444444;'>" + description + "</div></html>");
        descLbl.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        descWrapper.add(descLbl);
        panel.add(descWrapper);

        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBackground(Color.WHITE);
        wrapperPanel.add(panel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(wrapperPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        viewer.add(scrollPane);

        viewer.setVisible(true);
    }

    private void openEditDialog(File file, String[] meta){
        JDialog editDialog = new JDialog(this, "삽화 정보 수정", true);
        editDialog.setSize(400, 400);
        editDialog.setLocationRelativeTo(this);
        editDialog.setLayout(new BorderLayout());
        editDialog.getContentPane().setBackground(Color.WHITE);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 10, 0);
        gbc.weightx = 1.0;

        // 기존 데이터 불러오기
        JTextField txtTitle = new JTextField(meta[0]);
        JTextField txtChapter = new JTextField(meta[1]);
        String oldDesc = (meta.length > 2) ? meta[2].replace("<br>", "\n") : "";
        JTextArea txtDesc = new JTextArea(oldDesc, 4, 20);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(txtDesc);

        gbc.gridy = 0; formPanel.add(new JLabel("제목:"), gbc);
        gbc.gridy = 1; formPanel.add(txtTitle, gbc);
        gbc.gridy = 2; formPanel.add(new JLabel("회차 (예: 3 또는 ALL):"), gbc);
        gbc.gridy = 3; formPanel.add(txtChapter, gbc);
        gbc.gridy = 4; formPanel.add(new JLabel("설명:"), gbc);
        gbc.gridy = 5; formPanel.add(descScroll, gbc);

        editDialog.add(formPanel, BorderLayout.CENTER);

        JButton btnSave = new JButton("수정 완료");
        btnSave.setBackground(new Color(0, 140, 140));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        btnSave.addActionListener(e -> {
            if(txtTitle.getText().trim().isEmpty()){
                JOptionPane.showMessageDialog(editDialog, "제목을 입력해주세요.");
                return;
            }

            String cleanTitle = txtTitle.getText().replace("|", " ");
            String cleanChapter = txtChapter.getText().replace("|", " ");
            String cleanDesc = txtDesc.getText().replace("|", " ").replace("\n", "<br>");

            saveMapping(file.getName(), cleanTitle + "|" + cleanChapter + "|" + cleanDesc);
            refreshGallery();
            editDialog.dispose();
        });
        editDialog.add(btnSave, BorderLayout.SOUTH);
        editDialog.setVisible(true);
    }

    // 이미지 완전 삭제 엔진
    private void deleteImage(File file){
        int confirm = JOptionPane.showConfirmDialog(this, "목록에서 이 이미지를 삭제하시겠습니까?\n(폴더 안의 원본 사진은 지워지지 않습니다.)", "삭제 확인", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if(confirm == JOptionPane.YES_OPTION){
            String fileName = file.getName();

            // mapping.txt에서만 기록을 제거
            Map<String, String> map = loadMapping();
            map.remove(fileName);

            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(imageFolder + "\\mapping.txt"), "UTF-8"))) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    pw.println(entry.getKey() + "=" + entry.getValue());
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            refreshGallery();
            JOptionPane.showMessageDialog(this, "목록에서 삭제되었습니다.");
        }
    }

    // 폴더명으로 사용할 수 없는 특수문자를 제거하는 메서드
    private String getSafeFolderName(String title) {
        if (title == null) return "Unknown_Novel";
        // 윈도우 금지 문자뿐만 아니라 경로 구분자까지 모두 _ 로 치환
        return title.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    // 폴더를 역추적하여 현재 작품의 총 화수를 동적으로 계산하는 메서드
    private int getTotalChapters(){
        int count = 0;
        if(textFolderPath != null){
            File dir = new File(textFolderPath);
            if(dir.exists() && dir.isDirectory()){
                File[] files = dir.listFiles((d, name) ->
                        name.toLowerCase().endsWith(".txt") &&
                        !name.equals("bookmark.txt") &&
                        !name.equals("memo_bookmarks.txt")
                );
                if(files != null) count = files.length;
            }
        }

        return count > 0 ? count : 1;   //아무 파일도 없으면 최소 1 반환
    }
}
