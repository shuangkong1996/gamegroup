// 打工人语录
const quotes = [
  { text: '打工人，打工魂，打工都是人上人！', emoji: '💪' },
  { text: '今天搬砖不狠，明天地位不稳。', emoji: '🧱' },
  { text: '生活里80%的痛苦来源于打工，但不打工就有100%的痛苦。', emoji: '😭' },
  { text: '累吗？累就对了，舒服是留给有钱人的。', emoji: '😮‍💨' },
  { text: '打工可能会少活十年，不打工你一天也活不下去。', emoji: '⚰️' },
  { text: '不干活，就没饭吃，所以饿着也得干。', emoji: '🍚' },
  { text: '只要我够努力，老板一定会过上他想要的生活。', emoji: '🤑' },
  { text: '敢上九天揽月，敢下五洋捉鳖，但不敢迟到。', emoji: '⏰' },
  { text: '人可以没有爱情，但不能没有工资。', emoji: '💰' },
  { text: '头可断，血可流，工资不能少一毛。', emoji: '🩸' },
  { text: '没有困难的工作，只有勇敢的打工人。', emoji: '🦸' },
  { text: '上班的心情比上坟还沉重。', emoji: '💀' },
  { text: '工作使我快乐...呵呵。', emoji: '🙃' },
  { text: '我不是在莫庾，我是在做战略性休息。', emoji: '🐟' },
  { text: '表面在工作，实际已神游。', emoji: '👻' },
  { text: '工资是负担，莫庾才是刚需。', emoji: '🎣' },
  { text: '早安，打工人！今天也要假装很努力！', emoji: '☀️' },
  { text: '老板看我的眼神，像极了看免费劳动力。', emoji: '👀' },
  { text: '这不是莫庾，是工间休息，劳动法规定的。', emoji: '⚖️' },
  { text: '我对工作是真爱，真的很爱下班。', emoji: '❤️' },
  { text: '今日莫庾指南：假装思考，实则放空。', emoji: '🤔' },
  { text: '打工人的座右铭：能拖则拖，不能拖就磨。', emoji: '🐌' },
  { text: '我不是不努力，我是在蓄力。', emoji: '🔋' },
  { text: '下班了吗？没有！这四个字是我最不想听的。', emoji: '🙉' },
  { text: '工作嘛，差不多得了，又不是给自己干。', emoji: '🤷' }
];

/**
 * 获取随机语录
 */
function getRandomQuote() {
  const index = Math.floor(Math.random() * quotes.length);
  return quotes[index];
}

/**
 * 获取今日语录（每天固定）
 */
function getTodayQuote() {
  const today = new Date();
  const dayOfYear = Math.floor((today - new Date(today.getFullYear(), 0, 0)) / (1000 * 60 * 60 * 24));
  const index = dayOfYear % quotes.length;
  return quotes[index];
}

module.exports = {
  quotes,
  getRandomQuote,
  getTodayQuote
};
