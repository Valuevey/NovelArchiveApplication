import java.awt.Color;
import java.awt.Font;

//전체 패널이 공유하는 색상/폰트 상수 모음
public final class UiStyle {

    private UiStyle() {} // 인스턴스화 방지

    public static final Color COLOR_ACCENT = new Color(0, 140, 140);           // 포인트 청록색
    public static final Color COLOR_TEXT_INACTIVE = new Color(80, 85, 95);     // 비활성 텍스트
    public static final Color COLOR_MENU_HIGHLIGHT_BG = new Color(230, 245, 245); // 메뉴 선택 배경
    public static final Color COLOR_BG_LIGHT = new Color(248, 250, 252);       // 서재 배경색
    public static final Color COLOR_ICON_INACTIVE = new Color(140, 145, 155); // 비선택 아이콘

    public static final Font FONT_BOLD_12 = new Font("맑은 고딕", Font.BOLD, 12);
    public static final Font FONT_PLAIN_12 = new Font("맑은 고딕", Font.PLAIN, 12);
    public static final Font FONT_PLAIN_13 = new Font("맑은 고딕", Font.PLAIN, 13);
    public static final Font FONT_PLAIN_11 = new Font("맑은 고딕", Font.PLAIN, 11);

    public static final Color COLOR_LABEL_TEXT = new Color(50, 55, 60);      // 폼 라벨 텍스트 (10회+ 반복)
    public static final Color COLOR_BORDER_GRAY = new Color(225, 228, 232);  // 입력창 테두리 (여러 파일에서 로컬 변수로 중복 선언 중)
    public static final Font FONT_BOLD_13 = new Font("맑은 고딕", Font.BOLD, 13);
}