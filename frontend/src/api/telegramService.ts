/**
 * Telegram Bot API & Client Dispatch Service
 * Handles direct Telegram Bot API messaging, token configuration, and Telegram app deep-linking.
 */

export const HARDCODED_FALLBACK_BOT_TOKEN = '9199635370:AAFwqrszV4IrJmtbV1nbQnFQxOzzDxE9-OU';
export const HARDCODED_FALLBACK_BOT_USERNAME = 'kestrel_official_bot';

const STORAGE_KEY_TOKEN = 'kestrel_telegram_bot_token';
const STORAGE_KEY_DEFAULT_CHAT = 'kestrel_telegram_default_chat_id';

export function getStoredTelegramBotToken(): string {
  try {
    const stored = localStorage.getItem(STORAGE_KEY_TOKEN);
    if (stored && stored.trim()) return stored.trim();
  } catch {
    // LocalStorage fallback
  }
  return (import.meta.env.VITE_TELEGRAM_BOT_TOKEN as string) || HARDCODED_FALLBACK_BOT_TOKEN;
}

export function setStoredTelegramBotToken(token: string): void {
  try {
    if (token && token.trim()) {
      localStorage.setItem(STORAGE_KEY_TOKEN, token.trim());
    } else {
      localStorage.removeItem(STORAGE_KEY_TOKEN);
    }
  } catch {
    // LocalStorage fallback
  }
}

export function getStoredTelegramDefaultChatId(): string {
  try {
    const stored = localStorage.getItem(STORAGE_KEY_DEFAULT_CHAT);
    if (stored && stored.trim()) return stored.trim();
  } catch {
    // LocalStorage fallback
  }
  return '';
}

export function setStoredTelegramDefaultChatId(chatId: string): void {
  try {
    if (chatId && chatId.trim()) {
      localStorage.setItem(STORAGE_KEY_DEFAULT_CHAT, chatId.trim());
    } else {
      localStorage.removeItem(STORAGE_KEY_DEFAULT_CHAT);
    }
  } catch {
    // LocalStorage fallback
  }
}

export function getTelegramBotUsername(): string {
  return (import.meta.env.VITE_TELEGRAM_BOT_USERNAME as string) || HARDCODED_FALLBACK_BOT_USERNAME;
}

export interface TelegramChatDetectionResult {
  success: boolean;
  chatId?: string;
  username?: string;
  fullName?: string;
  text?: string;
  date?: string;
  error?: string;
}

/**
 * Polls Telegram Bot API getUpdates to auto-detect the user's Chat ID.
 * When a user opens @kestrel_official_bot and clicks /start (or sends any message),
 * this function grabs that incoming update and extracts their numeric chat ID!
 */
export async function fetchLatestTelegramChatId(customToken?: string): Promise<TelegramChatDetectionResult> {
  const token = (customToken || getStoredTelegramBotToken()).trim();
  const botUsername = getTelegramBotUsername();

  if (!token) {
    return {
      success: false,
      error: 'No Telegram bot token found.',
    };
  }

  try {
    const res = await fetch(`https://api.telegram.org/bot${token}/getUpdates?offset=-10&limit=10`);
    const data = await res.json().catch(() => null);

    if (!res.ok || !data?.ok) {
      return {
        success: false,
        error: data?.description || `HTTP ${res.status}`,
      };
    }

    const updates: any[] = data?.result || [];
    if (updates.length === 0) {
      return {
        success: false,
        error: `No recent messages found for @${botUsername}. Please open Telegram, search for @${botUsername}, and click START or send "Hi", then click Auto-Detect again!`,
      };
    }

    // Get the most recent update that contains a message or my_chat_member
    for (let i = updates.length - 1; i >= 0; i--) {
      const item = updates[i];
      const msg = item?.message || item?.edited_message || item?.channel_post;
      if (msg && msg.chat?.id) {
        const id = String(msg.chat.id);
        const username = msg.chat.username || msg.from?.username || '';
        const fullName = [msg.chat.first_name || msg.from?.first_name, msg.chat.last_name || msg.from?.last_name]
          .filter(Boolean)
          .join(' ');
        setStoredTelegramDefaultChatId(id);
        return {
          success: true,
          chatId: id,
          username,
          fullName,
          text: msg.text,
          date: msg.date ? new Date(msg.date * 1000).toISOString() : undefined,
        };
      }

      const member = item?.my_chat_member;
      if (member && member.chat?.id) {
        const id = String(member.chat.id);
        const username = member.chat.username || member.from?.username || '';
        const fullName = [member.chat.first_name || member.from?.first_name, member.chat.last_name || member.from?.last_name]
          .filter(Boolean)
          .join(' ');
        setStoredTelegramDefaultChatId(id);
        return {
          success: true,
          chatId: id,
          username,
          fullName,
        };
      }
    }

    return {
      success: false,
      error: `No chat found in updates. Please send /start to @${botUsername} and retry.`,
    };
  } catch (err: any) {
    return {
      success: false,
      error: `Network error connecting to Telegram: ${err?.message || 'unknown'}`,
    };
  }
}

export interface TelegramSendResult {
  success: boolean;
  status: 'DELIVERED' | 'FAILED' | 'NO_TOKEN' | 'CHAT_NOT_FOUND' | 'SIMULATED';
  message: string;
  errorDetail?: string;
  fallbackShareUrl: string;
  botStartUrl: string;
}

/**
 * Send an alert message to a Telegram user/chat via the Bot API.
 * If Bot Token is configured, calls https://api.telegram.org/bot<TOKEN>/sendMessage.
 * If Bot Token is absent, or chat is not found, provides instant deep-links to open Telegram.
 */
export async function sendTelegramMessage(
  recipient: string,
  messageText: string,
  customToken?: string
): Promise<TelegramSendResult> {
  const token = (customToken || getStoredTelegramBotToken()).trim();
  const botUsername = getTelegramBotUsername();
  const cleanRecipient = recipient.trim();

  const fallbackShareUrl = `https://t.me/share/url?url=${encodeURIComponent('https://kestrel.relay/')}&text=${encodeURIComponent(messageText)}`;
  const botStartUrl = `https://t.me/${botUsername.replace(/^@/, '')}?start=alert`;

  if (!token) {
    return {
      success: false,
      status: 'NO_TOKEN',
      message: 'No Telegram Bot Token configured. Configure your bot token or use the 1-click Telegram App share.',
      fallbackShareUrl,
      botStartUrl,
    };
  }

  // Telegram sendMessage requires chat_id. If recipient is empty, fallback to stored default
  const chatId = cleanRecipient || getStoredTelegramDefaultChatId();
  if (!chatId) {
    return {
      success: false,
      status: 'FAILED',
      message: 'Please provide your numeric Telegram Chat ID or username.',
      fallbackShareUrl,
      botStartUrl,
    };
  }

  try {
    const url = `https://api.telegram.org/bot${token}/sendMessage`;
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        chat_id: chatId,
        text: messageText,
        disable_web_page_preview: false,
      }),
    });

    const data = await response.json().catch(() => null);

    if (response.ok && data?.ok) {
      return {
        success: true,
        status: 'DELIVERED',
        message: `Alert message successfully delivered to Telegram chat (${chatId})! Message ID: ${data?.result?.message_id}`,
        fallbackShareUrl,
        botStartUrl,
      };
    }

    const description: string = data?.description || `HTTP ${response.status} ${response.statusText}`;

    // Handle common Telegram Bot API constraint: user hasn't /start'ed the bot or used @handle
    if (description.toLowerCase().includes('chat not found') || description.toLowerCase().includes('bot was blocked')) {
      const isHandle = cleanRecipient.startsWith('@');
      const guidance = isHandle
        ? `Telegram Error: "Bad Request: chat not found". Telegram bots cannot send messages to personal @handles. Telegram requires your numeric Chat ID, and you must click START on @${botUsername}. Click "Open @${botUsername} & Press START", then click "🔄 Auto-Detect Chat ID" or use the 1-Click Telegram App Send.`
        : `Telegram Error: "${description}". Telegram bots cannot send messages to a user until you start the bot. Click "Open @${botUsername} & Press START", then click "🔄 Auto-Detect Chat ID".`;

      return {
        success: false,
        status: 'CHAT_NOT_FOUND',
        message: guidance,
        errorDetail: description,
        fallbackShareUrl,
        botStartUrl,
      };
    }

    return {
      success: false,
      status: 'FAILED',
      message: `Telegram API error: ${description}`,
      errorDetail: description,
      fallbackShareUrl,
      botStartUrl,
    };
  } catch (err: any) {
    return {
      success: false,
      status: 'FAILED',
      message: `Failed to contact Telegram API: ${err?.message || 'Network error'}`,
      errorDetail: String(err),
      fallbackShareUrl,
      botStartUrl,
    };
  }
}

