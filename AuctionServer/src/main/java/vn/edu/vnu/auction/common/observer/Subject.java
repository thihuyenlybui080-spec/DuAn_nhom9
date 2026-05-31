package vn.edu.vnu.auction.common.observer;

/**
 * Interface Subject cho mẫu Observer.
 * <p>
 * Các lớp implement interface này duy trì danh sách các observer và thông báo cho họ khi trạng thái
 * thay đổi.
 * </p>
 */
public interface Subject {

  /**
   * Đăng ký một observer để nhận thông báo.
   *
   * @param observer observer cần đăng ký
   */
  void addObserver(Observer observer);

  /**
   * Xóa một observer khỏi danh sách thông báo.
   *
   * @param observer observer cần xóa
   */
  void removeObserver(Observer observer);

  /**
   * Thông báo cho tất cả observer đã đăng ký về thay đổi trạng thái.
   */
  void notifyObservers();
}