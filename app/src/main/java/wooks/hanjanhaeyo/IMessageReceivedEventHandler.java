package wooks.hanjanhaeyo;


public interface IMessageReceivedEventHandler
{
    void OnMessageReceived(GuestSession session, String sMessage);
}
