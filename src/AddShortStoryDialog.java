import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class AddShortStoryDialog extends JDialog {
    private JTextField tfTitle;
    private JTextField tfAuthor;
    private JTextField tfParentWork;    //소속 원작 타이틀 입력 필드
    private JTextField tfKeywords;
    private JTextArea taDescription;
    private JTextField tfFolderPath;

    private JComboBox<String> comboType;    //[단편], [썰], [연작] 선택용
    private JComboBox<String> comboPlatform;    //플랫폼 선택용(AppSettings 연동)

    private Snippet resultSnippet = null;
    private Color themeCyan = UiStyle.COLOR_ACCENT;   //청록색
    private Color borderGray = UiStyle.COLOR_BORDER_GRAY;

    public AddShortStoryDialog(Window parent){
        super(parent, "새 단편/썰 등록", ModalityType.APPLICATION_MODAL);

        setSize(580, 680);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        //메인 스크롤 콘텐츠 패널
        JPanel contentGridPanel = new JPanel();
        contentGridPanel.setBackground(Color.WHITE);
        contentGridPanel.setLayout(new BoxLayout(contentGridPanel, BoxLayout.Y_AXIS));
        contentGridPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 15, 25));

        //1. 소속 웑가 타이틀 행(좌측 카테고리 앵커 링킹용)
        contentGridPanel.add(createFormRow("소속 원작 작품명 *", tfParentWork = createCustomTextField("예: 콩쥐밭쥐, 백설공주")));
        contentGridPanel.add(Box.createVerticalStrut(12));

        //2. 작품 제목 행
        contentGridPanel.add(createFormRow("단편/썰 제목 *", tfTitle = createCustomTextField("")));
        contentGridPanel.add(Box.createVerticalStrut(12));

        //3. 작가 이름 행
        contentGridPanel.add(createFormRow("작가 닉네임", tfAuthor = createCustomTextField("")));
        contentGridPanel.add(Box.createVerticalStrut(12));

        //4. 콘텐츠 유형 및 출처 플랫폼(가로 2열 배치)
        JPanel rowComboPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        rowComboPanel.setBackground(Color.WHITE);
        rowComboPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        rowComboPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        //유형 콤보박스 패널
        JPanel typeWrapper = new JPanel(new BorderLayout(0, 5));
        typeWrapper.setBackground(Color.WHITE);
        JLabel lblType = new JLabel("콘텐츠 유형");
        lblType.setFont(UiStyle.FONT_BOLD_13);
        lblType.setForeground(UiStyle.COLOR_LABEL_TEXT);
        comboType = new JComboBox<>(new String[]{"단편", "썰", "연작"});
        styleComboBox(comboType);
        typeWrapper.add(lblType, BorderLayout.NORTH);
        typeWrapper.add(comboType, BorderLayout.CENTER);

        //플랫폼 콤보박스 패널(AppSettings에서 커스텀 플랫폼 로드)
        JPanel platformWrapper = new JPanel(new BorderLayout(0, 5));
        platformWrapper.setBackground(Color.WHITE);
        JLabel lblPlatform = new JLabel("출처 플랫폼");
        lblPlatform.setFont(UiStyle.FONT_BOLD_13);
        lblPlatform.setForeground(UiStyle.COLOR_LABEL_TEXT);

        ArrayList<String> platforms = AppSettings.getInstance().getCustomPlatforms();
        comboPlatform = new JComboBox<>(platforms.toArray(new String[0]));
        styleComboBox(comboPlatform);
        platformWrapper.add(lblPlatform, BorderLayout.NORTH);
        platformWrapper.add(comboPlatform, BorderLayout.CENTER);

        rowComboPanel.add(typeWrapper);
        rowComboPanel.add(platformWrapper);
        contentGridPanel.add(rowComboPanel);
        contentGridPanel.add(Box.createVerticalStrut(12));

        //5. 태그/키워드 행
        contentGridPanel.add(createFormRow("태그/키워드 (쉼표 구분)", tfKeywords = createCustomTextField("예: 회귀, 현대AU, 연반")));
        contentGridPanel.add(Box.createVerticalStrut(12));

        //6. 상세 메모/소개글 행
        JPanel rowDescPanel = new JPanel(new BorderLayout(0, 5));
        rowDescPanel.setBackground(Color.WHITE);
        rowDescPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowDescPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        JLabel lblDesc = new JLabel("작품 요약 및 주석 메모");
        lblDesc.setFont(UiStyle.FONT_BOLD_13);
        lblDesc.setForeground(UiStyle.COLOR_LABEL_TEXT);
        taDescription = new JTextArea();
        taDescription.setFont(UiStyle.FONT_PLAIN_12);
        taDescription.setLineWrap(true);
        JScrollPane descScroll = new JScrollPane(taDescription){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);

                g2.setColor(borderGray);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        descScroll.setOpaque(false);
        descScroll.getViewport().setOpaque(false);
        descScroll.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        rowDescPanel.add(lblDesc, BorderLayout.NORTH);
        rowDescPanel.add(descScroll, BorderLayout.CENTER);
        contentGridPanel.add(rowDescPanel);
        contentGridPanel.add(Box.createVerticalStrut(12));

        //7.  소설 텍스트 폴더 경로 지정 행
        JPanel rowFolderPanel = new JPanel(new BorderLayout(0, 5));
        rowFolderPanel.setBackground(Color.WHITE);
        rowFolderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowFolderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        JLabel lblFolder = new JLabel("텍스트 파일 보관 폴더 경로 *");
        lblFolder.setFont(UiStyle.FONT_BOLD_13);
        lblFolder.setForeground(UiStyle.COLOR_LABEL_TEXT);

        JPanel folderInputGroup = new JPanel(new BorderLayout(8, 0));
        folderInputGroup.setOpaque(false);
        tfFolderPath = createCustomTextField("");
        tfFolderPath.setEditable(false);
        tfFolderPath.setBackground(new Color(245, 246, 248));

        JButton btnBrowse = new JButton("찾기"){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);

                g2.setColor(themeCyan);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnBrowse.setFont(UiStyle.FONT_BOLD_12);
        btnBrowse.setForeground(themeCyan);
        btnBrowse.setContentAreaFilled(false);
        btnBrowse.setBorderPainted(false);
        btnBrowse.setPreferredSize(new Dimension(75, 32));
        btnBrowse.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBrowse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser("C:\\novel\\novels");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
                tfFolderPath.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        folderInputGroup.add(tfFolderPath, BorderLayout.CENTER);
        folderInputGroup.add(btnBrowse, BorderLayout.EAST);
        rowFolderPanel.add(lblFolder, BorderLayout.NORTH);
        rowFolderPanel.add(folderInputGroup, BorderLayout.CENTER);
        contentGridPanel.add(rowFolderPanel);

        //하단 제어 조작 버튼 바 패널(decor2 이미지 결합형)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(235, 238, 242)));
        bottomPanel.setBackground(new Color(245, 247, 249));
        bottomPanel.setPreferredSize(new Dimension(500, 75));

        JLabel lblDecor = new JLabel();
        File decorFile = new File("C:\\novel\\icon\\decor2.png");
        if(decorFile.exists()){
            try{
                ImageIcon icon = new ImageIcon("C:\\novel\\icon\\decor2.png");
                Image scaled = icon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                lblDecor.setIcon(new ImageIcon(scaled));
            } catch(Exception e){}
        }
        lblDecor.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        bottomPanel.add(lblDecor, BorderLayout.WEST);

        JPanel btnGroupSuite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 18));
        btnGroupSuite.setOpaque(false);

        JButton btnCancel = new JButton("취소"){
            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);

                g2.setColor(themeCyan);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnCancel.setFont(UiStyle.FONT_BOLD_13);
        btnCancel.setForeground(themeCyan);
        btnCancel.setContentAreaFilled(false);
        btnCancel.setBorderPainted(false);
        btnCancel.setPreferredSize(new Dimension(95, 36));
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = new JButton("등록"){
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(themeCyan);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnSave.setFont(UiStyle.FONT_BOLD_13);
        btnSave.setForeground(Color.WHITE);
        btnSave.setContentAreaFilled(false);
        btnSave.setBorderPainted(false);
        btnSave.setPreferredSize(new Dimension(135, 36));
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.addActionListener(e -> {
            String parentWork = tfParentWork.getText().trim();
            String title = tfTitle.getText().trim();
            String author = tfAuthor.getText().trim();
            String folderPath = tfFolderPath.getText().trim();

            if(parentWork.isEmpty() || title.isEmpty() || folderPath.isEmpty()){
                JOptionPane.showMessageDialog(this, "소속 원작, 작품 제목, 파일 폴더 경로는 필수 입력 항목입니다", "경고", JOptionPane.WARNING_MESSAGE);
                return;
            }

            //폴더 내의 파일 크기를 기반으로 대략적인 더미 글자 수(wordCount)연산 산출
            int calculatedWorkCount = calculateEstimatedWordCount(folderPath);

            resultSnippet = new Snippet(
                    title, author.isEmpty() ? "작자미상" : author,
                    (String) comboPlatform.getSelectedItem(), folderPath, parentWork,
                    (String) comboType.getSelectedItem(), calculatedWorkCount,
                    tfKeywords.getText().trim(), taDescription.getText().trim(),
                    "기록 없음", false
            );
            dispose();
        });

        btnGroupSuite.add(btnCancel);
        btnGroupSuite.add(btnSave);
        bottomPanel.add(btnGroupSuite, BorderLayout.EAST);

        add(contentGridPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    //파일 용량 기반 대략적 글자 수 추출 연산 서포터
    private int calculateEstimatedWordCount(String folderPath){
        try{
            File dir = new File(folderPath);
            if(dir.exists() && dir.isDirectory()){
                File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".txt"));
                if(files != null){
                    long totalBytes = 0;
                    for(File f : files) totalBytes += f.length();
                    //UTF-8 혹은 한글 텍스트 기준 대략 2~3바아티그 1자이므로, 2.5로 나누어 자수 유추
                    return (int) (totalBytes/2.5);
                }
            }
        } catch(Exception e){}
        return 0;
    }

    private JTextField createCustomTextField(String placeholder){
        JTextField tf = new JTextField(){
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
        tf.setOpaque(false);
        tf.setFont(UiStyle.FONT_PLAIN_12);
        tf.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        return tf;
    }

    private JPanel createFormRow(String title, JTextField textField){
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        JLabel label = new JLabel(title);
        label.setFont(UiStyle.FONT_BOLD_13);
        label.setForeground(UiStyle.COLOR_LABEL_TEXT);

        textField.setPreferredSize(new Dimension(Integer.MAX_VALUE, 32));

        panel.add(label, BorderLayout.NORTH);
        panel.add(textField, BorderLayout.CENTER);
        return panel;
    }

    private void styleComboBox(JComboBox<String> combo){
        combo.setFont(UiStyle.FONT_PLAIN_12);
        combo.setBackground(Color.WHITE);
        combo.setPreferredSize(new Dimension(combo.getPreferredSize().width, 32));
    }

    public Snippet getResultSnippet(){
        return resultSnippet;
    }
}
