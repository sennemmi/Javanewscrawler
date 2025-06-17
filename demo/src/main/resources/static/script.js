// DOM元素获取
const getEl = (id) => document.getElementById(id);
const urlInput = getEl('urlInput');
const crawlBtn = getEl('crawlBtn');
const saveBtn = getEl('saveBtn');
const resultContent = getEl('resultContent');
const historyList = getEl('historyList');
const userStatus = getEl('userStatus');
const userInfo = userStatus.querySelector('.user-info');
const authButtons = userStatus.querySelector('.auth-buttons');
const usernameSpan = userInfo.querySelector('.username');
const logoutBtn = userInfo.querySelector('.logout');
const saveDialog = getEl('saveDialog');
const overlay = getEl('overlay');
const closeSaveDialog = getEl('closeSaveDialog');
const savePdf = getEl('savePdf');
const saveWord = getEl('saveWord');
const sidebar = getEl('sidebar');
const toggleSidebar = getEl('toggleSidebar');
const mainContent = getEl('mainContent');
const searchContainer = getEl('searchContainer');
const contentArea = getEl('contentArea');

// UI交互: 侧边栏, 关于, 收藏夹
const aboutItem = getEl('aboutItem');
const aboutContent = getEl('aboutContent');
const favoritesItem = getEl('favoritesItem');
const favoritesContent = getEl('favoritesContent');
const favoritesList = getEl('favoritesList');
const addFavoriteBtn = getEl('addFavoriteBtn');
const favoriteDialog = getEl('favoriteDialog');
const closeFavoriteDialog = getEl('closeFavoriteDialog');
const cancelFavorite = getEl('cancelFavorite');
const saveFavorite = getEl('saveFavorite');
const favoriteUrlInput = getEl('favoriteUrlInput');

// ----------------- UI交互逻辑 -----------------

// 侧边栏切换
toggleSidebar.addEventListener('click', () => {
    sidebar.classList.toggle('collapsed');
    localStorage.setItem('sidebarCollapsed', sidebar.classList.contains('collapsed'));
});
if (localStorage.getItem('sidebarCollapsed') === 'true') {
    sidebar.classList.add('collapsed');
}

// "关于我们" 和 "收藏夹" 展开/收起
[aboutItem, favoritesItem].forEach(item => {
    item.addEventListener('click', () => {
        const content = getEl(item.id.replace('Item', 'Content'));
        item.classList.toggle('active');
        content.classList.toggle('active');
    });
});

// 显示/隐藏弹窗
const showDialog = (dialog) => {
    dialog.classList.add('active');
    overlay.classList.add('active');
};
const hideDialog = (dialog) => {
    dialog.classList.remove('active');
    overlay.classList.remove('active');
};

saveBtn.addEventListener('click', () => urlInput.value.trim() ? showDialog(saveDialog) : alert('请先输入网址并成功爬取！'));
closeSaveDialog.addEventListener('click', () => hideDialog(saveDialog));
addFavoriteBtn.addEventListener('click', () => {
    favoriteUrlInput.value = urlInput.value.trim();
    showDialog(favoriteDialog);
});
closeFavoriteDialog.addEventListener('click', () => hideDialog(favoriteDialog));
cancelFavorite.addEventListener('click', () => hideDialog(favoriteDialog));
overlay.addEventListener('click', () => {
    hideDialog(saveDialog);
    hideDialog(favoriteDialog);
});

// ----------------- 用户认证 & 历史记录 (后端API) -----------------

// 检查用户登录状态并加载历史
async function checkLoginStatusAndLoadData() {
    try {
        const response = await fetch('/api/user/current');
        if (response.ok) {
            const user = await response.json();
            userInfo.style.display = 'flex';
            authButtons.style.display = 'none';
            usernameSpan.textContent = user.username;
            await fetchAndDisplayHistory();
        } else {
            handleLogoutState();
        }
    } catch (error) {
        console.error("检查登录状态失败:", error);
        handleLogoutState();
    }
}

function handleLogoutState() {
    userInfo.style.display = 'none';
    authButtons.style.display = 'flex';
    usernameSpan.textContent = '';
    historyList.innerHTML = '<li style="padding: 10px; color: #64748b; cursor: default;">请登录以查看历史记录</li>';
}

logoutBtn.addEventListener('click', () => {
    // 在实际应用中，这里应该调用后端的 /logout 接口
    // 为了演示，我们假设退出成功
    handleLogoutState();
    // 可以重定向到登录页
    // window.location.href = '/login.html'; 
    alert("您已退出登录。");
});

// 从后端获取并显示历史记录
async function fetchAndDisplayHistory() {
    try {
        const response = await fetch('/api/history');
        if (response.ok) {
            const historyData = await response.json();
            updateHistoryDisplay(historyData);
        } else if (response.status !== 401) {
            console.error('获取历史记录失败:', response.status);
            historyList.innerHTML = '<li style="padding: 10px; color: #ef4444; cursor: default;">加载历史失败</li>';
        }
    } catch (error) {
        console.error('获取历史记录时发生错误:', error);
    }
}

// 更新历史记录UI
function updateHistoryDisplay(history) {
    if (!history || history.length === 0) {
        historyList.innerHTML = '<li style="padding: 10px; color: #64748b; cursor: default;">暂无历史记录</li>';
        return;
    }
    historyList.innerHTML = history.map(item => `
        <li class="history-item" onclick="loadHistoryItem('${item.url}')">
            <div class="history-content">
                <span class="title">${item.title}</span>
                <span class="time">${new Date(item.crawlTime).toLocaleString()}</span>
            </div>
        </li>
    `).join('');
}

// 点击历史记录项加载
function loadHistoryItem(url) {
    urlInput.value = url;
    crawlBtn.click();
}

// ----------------- 核心爬取 & 导出功能 (后端API) -----------------

// 爬取网页
crawlBtn.addEventListener('click', async () => {
    const url = urlInput.value.trim();
    if (!url) {
        alert('请输入要爬取的网址！');
        return;
    }

    searchContainer.classList.add('active');
    contentArea.classList.add('active');
    resultContent.innerHTML = '<span><div class="loading"></div>正在联系服务器爬取...</span>';

    try {
        const response = await fetch('/api/crawl/single', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ url })
        });

        if (response.ok) {
            const newsData = await response.json();
            displaySuccessResult(newsData);
            await fetchAndDisplayHistory(); // 成功后刷新历史记录
        } else {
            const errorText = await response.text();
            let errorMessage = `请求失败 (状态码: ${response.status})`;
            if (response.status === 401) errorMessage = "未登录或会话已过期，请重新登录。";
            else if (response.status === 400) errorMessage = `请求无效：${errorText || 'URL格式可能不正确。'}`;
            else if (response.status === 500) errorMessage = `服务器内部错误：${errorText || '爬取或存储时发生问题。'}`;
            
            displayErrorResult(errorMessage);
        }
    } catch (error) {
        console.error('爬取错误:', error);
        displayErrorResult('无法连接到服务器，请检查网络或后端服务是否正在运行。');
    }
});

function displaySuccessResult(data) {
    const resultHtml = `
        <div class="result-content">
            <div>
                <strong>标题</strong>
                <p>${data.title || 'N/A'}</p>
            </div>
            <div>
                <strong>来源</strong>
                <p>${data.source || 'N/A'}</p>
            </div>
            <div>
                <strong>发布时间</strong>
                <p>${data.publishTime ? new Date(data.publishTime).toLocaleString() : 'N/A'}</p>
            </div>
             <div>
                <strong>关键词</strong>
                <p>${data.keywords || '无'}</p>
            </div>
            <div>
                <strong>正文内容</strong>
                <div class="content-html">${data.content || '无内容'}</div>
            </div>
        </div>
    `;
    resultContent.innerHTML = resultHtml;
}

function displayErrorResult(message) {
     const errorHtml = `
        <div style="color: #ef4444; padding: 20px; background: rgba(239, 68, 68, 0.1); border-radius: 8px;">
            <strong style="color: #ef4444;">爬取失败</strong>
            <div style="margin-top: 8px;">${message}</div>
        </div>
    `;
    resultContent.innerHTML = errorHtml;
}

// 导出文件
const exportFile = async (format) => {
    const url = urlInput.value.trim();
    if (!url) {
        alert('请先指定一个要导出的URL！');
        return;
    }
    try {
        const response = await fetch(`/api/export?url=${encodeURIComponent(url)}&format=${format}`);
        if (response.ok) {
            const blob = await response.blob();
            const contentDisposition = response.headers.get('Content-Disposition');
            let filename = `export.${format === 'word' ? 'docx' : 'pdf'}`;
            if (contentDisposition) {
                const filenameMatch = contentDisposition.match(/filename="(.+?)"/);
                if (filenameMatch && filenameMatch.length > 1) {
                    filename = decodeURIComponent(filenameMatch[1]);
                }
            }
            const link = document.createElement('a');
            link.href = URL.createObjectURL(blob);
            link.download = filename;
            link.click();
            URL.revokeObjectURL(link.href);
            alert(`${format.toUpperCase()} 文件已开始下载！`);
        } else {
             let errorText = await response.text();
             let errorMessage = `导出失败 (状态码: ${response.status})`;
             if (response.status === 401) errorMessage = "未登录或会话已过期，无法导出。";
             else errorMessage = `导出失败: ${errorText}`;
             alert(errorMessage);
        }
    } catch (error) {
        console.error(`导出${format}错误:`, error);
        alert(`导出${format.toUpperCase()}时发生网络错误。`);
    }
    hideDialog(saveDialog);
};

savePdf.addEventListener('click', () => exportFile('pdf'));
saveWord.addEventListener('click', () => exportFile('word'));

urlInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') crawlBtn.click();
});

// ----------------- 收藏夹 (本地LocalStorage实现) -----------------
let favorites = JSON.parse(localStorage.getItem('favorites')) || [];

function updateFavoritesDisplay() {
    if (favorites.length === 0) {
        favoritesList.innerHTML = '<div class="empty-favorites">暂无收藏</div>';
    } else {
        favoritesList.innerHTML = favorites.map((item, index) => `
            <li class="favorite-item">
                <a href="${item.url}" target="_blank" class="favorite-url" title="${item.url}">${item.url}</a>
                <span class="favorite-btn" onclick="deleteFavorite(${index})">
                     <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                </span>
            </li>
        `).join('');
    }
}

function addFavorite(url) {
    if (!url) return;
    if (favorites.some(item => item.url === url)) {
        alert('该网址已收藏！');
        return;
    }
    favorites.unshift({ url });
    localStorage.setItem('favorites', JSON.stringify(favorites));
    updateFavoritesDisplay();
}

function deleteFavorite(index) {
    favorites.splice(index, 1);
    localStorage.setItem('favorites', JSON.stringify(favorites));
    updateFavoritesDisplay();
}

saveFavorite.addEventListener('click', () => {
    const url = favoriteUrlInput.value.trim();
    if (!url) {
        alert('请输入要收藏的网址！');
        return;
    }
    addFavorite(url);
    hideDialog(favoriteDialog);
});

favoriteUrlInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') saveFavorite.click();
});

// ----------------- 初始化 -----------------
document.addEventListener('DOMContentLoaded', () => {
    checkLoginStatusAndLoadData();
    updateFavoritesDisplay();
});