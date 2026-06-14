public class Main{
  public static void main(String[] args){
    //프로그램 시작하면 보관함 객체를 생성하면 화면에 띄움
    BookShelfPage bookShelfPage = new BookShelfPage();
    bookShelfPage.openBookShelf();
  }
}

/* 수정 사항 및 추가
○ 1. 프로그램 시작할 때마다 시작일이 리셋 되는 것
○ 2. 검색창에 ⓘ로 마우스포인터 올려놓으면 검색 방법 뜨게(마우스포인터 치우면 사라짐)
○ 3. 환경 설정에 전체 삭제 버튼 위치 옮기기
4. .exe 만들기
○ 5. 소설 정보창에서 표지 클릭하면 표지 이미지만 크게 띄워서 보여주기
○ 6. 뷰어 설정에서 (-)[숫자](+)로 글자 크기 조절할 수 있게 하기

 */