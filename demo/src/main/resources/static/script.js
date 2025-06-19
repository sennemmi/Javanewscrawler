// DOM元素获取
const getEl = (id) => document.getElementById(id);

// 页面视图
const homeView = getEl('homeView');
const resultView = getEl('resultView');
const articleWrapper = getEl('articleWrapper');

// 控件
const urlInput = getEl('urlInput');
const crawlBtn = getEl('crawlBtn');
const backBtn = getEl('backBtn');
const exportBtn = getEl('exportBtn');
const historyList = getEl('historyList');
const sidebar = getEl('sidebar');
const toggleSidebar = getEl('toggleSidebar');
const typeOptions = document.querySelectorAll('.type-option');
const searchTip = getEl('searchTip');
const keywordOptions = getEl('keywordOptions');
const keywordEntryUrl = getEl('keywordEntryUrl');

// 用户认证
const userStatus = getEl('userStatus');
const userInfo = userStatus.querySelector('.user-info');
const authButtons = userStatus.querySelector('.auth-buttons');
const usernameSpan = userInfo.querySelector('.username');
const logoutBtn = userInfo.querySelector('.logout');

// 对话框
const overlay = getEl('overlay');
const saveDialog = getEl('saveDialog');
const closeSaveDialog = getEl('closeSaveDialog');
const cancelExport = getEl('cancelExport');
const confirmExport = getEl('confirmExport');
const favoriteDialog = getEl('favoriteDialog');
const closeFavoriteDialog = getEl('closeFavoriteDialog');
const cancelFavorite = getEl('cancelFavorite');
const saveFavorite = getEl('saveFavorite');
const favoriteUrlInput = getEl('favoriteUrlInput');

// 导出设置控件
const exportPdfBtn = getEl('exportPdfBtn');
const exportWordBtn = getEl('exportWordBtn');
const fontFamily = getEl('fontFamily');
const textFontSize = getEl('textFontSize');
const lineSpacing = getEl('lineSpacing');
const pageWidth = getEl('pageWidth');
const titleFontSize = getEl('titleFontSize');
const h1FontSize = getEl('h1FontSize');
const h2FontSize = getEl('h2FontSize');
const h3FontSize = getEl('h3FontSize');
const captionFontSize = getEl('captionFontSize');
const footerFontSize = getEl('footerFontSize');

// 侧边栏可展开项
const aboutItem = getEl('aboutItem');
const favoritesItem = getEl('favoritesItem');
const addFavoriteBtn = getEl('addFavoriteBtn');
const favoritesList = getEl('favoritesList');

// 存储当前成功爬取的文章数据，用于导出
let currentArticleData = null;
// 存储当前选择的导出格式
let currentExportFormat = 'pdf';
// 当前爬取类型
let currentCrawlType = 'single';

// 分析按钮
const analysisBtn = getEl('analysisBtn');

// ----------------- UI 交互逻辑 -----------------

// 视图切换
const showView = (view) => {
    homeView.style.display = 'none';
    resultView.style.display = 'none';
    view.style.display = 'block';
    
    // 如果切换到首页，重置按钮状态
    if (view === homeView) {
        exportBtn.style.display = 'flex';
        analysisBtn.style.display = 'none';
    }
};

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

exportBtn.addEventListener('click', () => {
    if (!currentArticleData || !currentArticleData.url) {
        alert('没有可导出的内容。请先成功爬取一篇文章。');
        return;
    }
    showDialog(saveDialog);
});
closeSaveDialog.addEventListener('click', () => hideDialog(saveDialog));
cancelExport.addEventListener('click', () => hideDialog(saveDialog));
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
backBtn.addEventListener('click', () => {
    showView(homeView);
});

// 爬取类型切换
typeOptions.forEach(option => {
    option.addEventListener('click', () => {
        // 更新激活的选项
        typeOptions.forEach(opt => opt.classList.remove('active'));
        option.classList.add('active');
        
        // 获取爬取类型
        currentCrawlType = option.getAttribute('data-type');
        
        // 更新搜索框提示和占位符
        updateInputForCrawlType();
    });
});

// 根据爬取类型更新输入框和提示
function updateInputForCrawlType() {
    // 关键词爬取选项显示控制
    keywordOptions.style.display = currentCrawlType === 'keyword' ? 'block' : 'none';
    
    // 更新输入框提示文本
    switch(currentCrawlType) {
        case 'single':
            urlInput.placeholder = "输入新浪新闻网址...";
            searchTip.textContent = "输入单个新闻URL，如 https://news.sina.com.cn/c/xxxx/doc-xxx.shtml";
            break;
        case 'index':
            urlInput.placeholder = "输入新闻列表页或频道页网址...";
            searchTip.textContent = "输入新闻列表页URL，如 https://news.sina.com.cn/china/ 或 https://news.sina.com.cn/c/2023-10-15/";
            break;
        case 'keyword':
            urlInput.placeholder = "输入关键词...";
            searchTip.textContent = "输入要搜索的关键词，系统将从指定入口页中爬取包含该关键词的新闻";
            break;
    }
}

// ----------------- 核心爬取 & 导出功能 (后端API) -----------------

// 爬取网页
crawlBtn.addEventListener('click', async () => {
    const input = urlInput.value.trim();
    if (!input) {
        alert('请输入' + (currentCrawlType === 'keyword' ? '关键词' : '网址') + '！');
        return;
    }
    
    showView(resultView);
    articleWrapper.innerHTML = '<div class="loading-container"><span><div class="loading"></div>正在联系服务器爬取...</span></div>';
    
    try {
        // 根据不同爬取类型选择不同的API端点和处理方式
        let response;
        
        switch(currentCrawlType) {
            case 'single':
                response = await fetch('/api/crawl/single', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ url: input })
                });
                break;
                
            case 'index':
                response = await fetch('/api/crawl/from-index', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ url: input })
                });
                break;
                
            case 'keyword':
                const payload = { keyword: input };
                if (keywordEntryUrl.value.trim()) {
                    payload.url = keywordEntryUrl.value.trim();
                }
                
                response = await fetch('/api/crawl/by-keyword', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                break;
        }
        
        if (response.ok) {
            const data = await response.json();
            
            // 单URL爬取和批量爬取的显示方式不同
            if (currentCrawlType === 'single') {
                displaySuccessResult(data);
            } else {
                displayBatchCrawlResult(data, currentCrawlType);
            }
            
            await fetchAndDisplayHistory(); // 成功后刷新历史记录
        } else {
            const errorText = await response.text();
            let errorMessage = `请求失败 (状态码: ${response.status})`;
            if (response.status === 401) errorMessage = "未登录或会话已过期，请重新登录。";
            else if (response.status === 400) errorMessage = `请求无效：${errorText || '输入格式可能不正确。'}`;
            else if (response.status === 500) errorMessage = `服务器内部错误：${errorText || '爬取或存储时发生问题。'}`;
            
            displayErrorResult(errorMessage);
        }
    } catch (error) {
        console.error('爬取错误:', error);
        displayErrorResult('无法连接到服务器，请检查网络或后端服务是否正在运行。');
    }
});

// 显示单URL爬取的结果
function displaySuccessResult(data) {
    currentArticleData = data; // 缓存数据用于导出
    
    // 显示导出按钮，隐藏分析按钮
    exportBtn.style.display = 'flex';
    analysisBtn.style.display = 'none';
    
    const resultHtml = `
        <header class="article-header">
            <h1>${data.title || '无标题'}</h1>
            <div class="article-meta">
                ${data.source ? `<div class="meta-item"><strong>来源:</strong> <span>${data.source}</span></div>` : ''}
                ${data.author ? `<div class="meta-item"><strong>作者:</strong> <span>${data.author}</span></div>` : ''}
                ${data.publishTime ? `<div class="meta-item"><strong>发布时间:</strong> <span>${new Date(data.publishTime).toLocaleString()}</span></div>` : ''}
                ${data.keywords ? `<div class="meta-item"><strong>关键词:</strong> <span>${data.keywords}</span></div>` : ''}
            </div>
        </header>
        <div class="article-body">
            ${data.content || '<p>无正文内容。</p>'}
        </div>
        <div class="article-actions">
            <button class="btn btn-primary" onclick="showDialog(saveDialog)">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 5px;">
                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                    <polyline points="17 21 17 13 7 13 7 21"></polyline>
                    <polyline points="7 3 7 8 15 8"></polyline>
                </svg>
                导出文档
            </button>
            ${data.crawlHistoryId ? `
            <button class="btn btn-secondary" onclick="goToAnalysisPage(${data.crawlHistoryId})">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 5px;">
                    <path d="M18 20V10"></path>
                    <path d="M12 20V4"></path>
                    <path d="M6 20v-6"></path>
                </svg>
                数据分析
            </button>
            ` : ''}
        </div>
    `;
    articleWrapper.innerHTML = resultHtml;
}

// 显示批量爬取结果（二级爬取或关键词爬取）
function displayBatchCrawlResult(data, type) {
    currentArticleData = null; // 批量爬取不支持导出单篇文章
    
    // 根据不同类型设置标题和信息
    let title, info;
    if (type === 'index') {
        title = "二级爬取完成";
        info = `从入口页 <strong>${data.entryUrl}</strong> 成功爬取了 <strong>${data.crawledCount}</strong> 条新闻`;
    } else if (type === 'keyword') {
        title = "关键词爬取完成";
        info = `使用关键词 <strong>${data.keyword}</strong> 从 <strong>${data.entryUrl}</strong> 成功爬取了 <strong>${data.crawledCount}</strong> 条新闻`;
    }
    
    // 获取最新的历史记录ID
    getLatestHistoryId().then(historyId => {
        // 显示分析按钮，隐藏导出按钮
        if (historyId) {
            exportBtn.style.display = 'none';
            analysisBtn.style.display = 'flex';
            analysisBtn.onclick = () => goToAnalysisPage(historyId);
        } else {
            exportBtn.style.display = 'none';
            analysisBtn.style.display = 'none';
        }
        
        // 构建结果HTML
        const resultHtml = `
            <div class="batch-result-summary">
                <h2 class="batch-result-title">${title}</h2>
                <div class="batch-result-info">${info}</div>
                <div class="batch-result-tip">爬取结果已添加到历史记录，您可以通过侧边栏访问这些新闻</div>
                
                ${data.titles && data.titles.length > 0 ? `
                    <div class="batch-result-list">
                        ${data.titles.map((title, index) => `
                            <div class="batch-result-item">${index + 1}. ${title}</div>
                        `).join('')}
                    </div>
                ` : ''}
            </div>
            <div class="batch-result-actions">
                <button class="btn btn-primary" onclick="showView(homeView)">返回首页继续爬取</button>
            </div>
        `;
        
        articleWrapper.innerHTML = resultHtml;
    }).catch(error => {
        console.error('获取历史ID失败:', error);
        
        // 隐藏导出和分析按钮
        exportBtn.style.display = 'none';
        analysisBtn.style.display = 'none';
        
        // 构建无历史ID的结果HTML
        const resultHtml = `
            <div class="batch-result-summary">
                <h2 class="batch-result-title">${title}</h2>
                <div class="batch-result-info">${info}</div>
                <div class="batch-result-tip">爬取结果已添加到历史记录，您可以通过侧边栏访问这些新闻</div>
                
                ${data.titles && data.titles.length > 0 ? `
                    <div class="batch-result-list">
                        ${data.titles.map((title, index) => `
                            <div class="batch-result-item">${index + 1}. ${title}</div>
                        `).join('')}
                    </div>
                ` : ''}
            </div>
            <div class="batch-result-actions">
                <button class="btn btn-primary" onclick="showView(homeView)">返回首页继续爬取</button>
            </div>
        `;
        
        articleWrapper.innerHTML = resultHtml;
    });
}

// 获取最新的历史记录ID
async function getLatestHistoryId() {
    try {
        const response = await fetch('/api/history');
        if (response.ok) {
            const historyData = await response.json();
            if (historyData && historyData.length > 0) {
                // 返回最新的历史记录ID（第一条记录）
                return historyData[0].id;
            }
        }
        return null;
    } catch (error) {
        console.error('获取历史记录失败:', error);
        return null;
    }
}

// 跳转到数据分析页面，并传递历史ID参数
function goToAnalysisPage(historyId) {
    // 不再跳转到单独的页面，而是在当前页面显示数据分析视图
    if (window.dataAnalysis && typeof window.dataAnalysis.startAnalysis === 'function') {
        window.dataAnalysis.startAnalysis(historyId);
    } else {
        console.error('数据分析功能未加载');
        alert('数据分析功能暂时不可用');
    }
}

function displayErrorResult(message) {
    currentArticleData = null; // 清除缓存
    const errorHtml = `
        <div class="error-container">
            <strong>爬取失败</strong>
            <p>${message}</p>
        </div>
    `;
    articleWrapper.innerHTML = errorHtml;
}

urlInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') crawlBtn.click();
});

// 导出文件
confirmExport.addEventListener('click', async () => {
    if (!currentArticleData || !currentArticleData.url) {
        alert('没有可导出的内容。请先成功爬取一篇文章。');
        return;
    }
    
    try {
        // 构建URL参数
        const url = currentArticleData.url;
        const format = currentExportFormat;
        const textSize = textFontSize.value;
        const spacing = lineSpacing.value;
        const font = encodeURIComponent(fontFamily.value);
        const width = pageWidth.value;
        
        // 构建完整URL，包含所有参数
        let exportUrl = `/api/export?url=${encodeURIComponent(url)}&format=${format}&textFontSize=${textSize}&lineSpacing=${spacing}&fontFamily=${font}&pageWidth=${width}`;
        
        // 添加高级设置参数
        exportUrl += `&titleFontSize=${titleFontSize.value}`;
        exportUrl += `&h1FontSize=${h1FontSize.value}`;
        exportUrl += `&h2FontSize=${h2FontSize.value}`;
        exportUrl += `&h3FontSize=${h3FontSize.value}`;
        exportUrl += `&captionFontSize=${captionFontSize.value}`;
        exportUrl += `&footerFontSize=${footerFontSize.value}`;
        
        // 添加skipHistoryRecord参数，告知后端不要记录导出历史
        exportUrl += `&skipHistoryRecord=true`;
        
        // 显示加载状态
        confirmExport.textContent = '导出中...';
        confirmExport.disabled = true;
        
        // 使用GET方法发送请求，因为后端当前只支持GET方式的导出
        const response = await fetch(exportUrl);
        
        // 恢复按钮状态
        confirmExport.textContent = '确认导出';
        confirmExport.disabled = false;
        
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
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(link.href);
            
            alert(`${format.toUpperCase()} 文件已开始下载！`);
            hideDialog(saveDialog);
        } else {
            const errorText = await response.text();
            alert(`导出失败 (状态码: ${response.status}): ${errorText}`);
        }
    } catch (error) {
        confirmExport.textContent = '确认导出';
        confirmExport.disabled = false;
        console.error('导出错误:', error);
        alert(`导出过程中发生错误: ${error.message}`);
    }
});

// 导出格式选择
exportPdfBtn.addEventListener('click', () => {
    exportPdfBtn.classList.add('active');
    exportWordBtn.classList.remove('active');
    currentExportFormat = 'pdf';
});

exportWordBtn.addEventListener('click', () => {
    exportWordBtn.classList.add('active');
    exportPdfBtn.classList.remove('active');
    currentExportFormat = 'word';
});

// 不再需要高级设置切换代码，因为我们现在使用左右布局

// ----------------- 用户认证 & 历史记录 (后端API) -----------------

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
    historyList.innerHTML = '<li style="padding: 10px; color: var(--text-color-secondary);">请登录以查看历史记录</li>';
}

logoutBtn.addEventListener('click', () => {
    handleLogoutState();
    alert("您已退出登录。");
});

async function fetchAndDisplayHistory() {
    try {
        const response = await fetch('/api/history');
        if (response.ok) {
            const historyData = await response.json();
            updateHistoryDisplay(historyData);
        } else if (response.status !== 401) {
            historyList.innerHTML = '<li style="padding: 10px; color: #ef4444;">加载历史失败</li>';
        }
    } catch (error) {
        console.error('获取历史记录时发生错误:', error);
    }
}

function updateHistoryDisplay(history) {
    if (!history || history.length === 0) {
        historyList.innerHTML = '<li style="padding: 10px; color: var(--text-color-secondary);">暂无历史记录</li>';
        return;
    }
    historyList.innerHTML = history.map(item => {
        // 确保URL是原始URL，不要再额外编码
        const url = item.url || '';
        
        // 判断是否为批量爬取（二级爬取或关键词爬取）
        let isBatchCrawl = item.crawlType === 'INDEX_CRAWL' || item.crawlType === 'KEYWORD_CRAWL';
        
        // 尝试解析params字段
        let params = null;
        if (item.params) {
            try {
                params = JSON.parse(item.params);
            } catch (e) {
                console.error('解析params失败:', e);
            }
        }
        
        // 如果是INDEX_CRAWL类型且params中包含totalCount，则认为是批量爬取
        if (item.crawlType === 'INDEX_CRAWL' && params && params.totalCount) {
            isBatchCrawl = true;
        }
        
        // 根据是否为批量爬取决定点击事件
        const clickHandler = isBatchCrawl 
            ? `onclick="loadNewsByHistoryId(${item.id}, event)"` 
            : `onclick="loadHistoryItem('${url}')"`;
        
        return `
        <li class="history-item" data-id="${item.id}">
            <div class="history-content" ${clickHandler}>
                <span class="title">${item.title}</span>
                <span class="time">${new Date(item.crawlTime).toLocaleString()}</span>
            </div>
            <div class="history-actions">
                ${isBatchCrawl ? `
                <div class="view-news-btn" onclick="loadNewsByHistoryId(${item.id}, event)" title="查看关联新闻">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                        <polyline points="14 2 14 8 20 8"></polyline>
                        <line x1="16" y1="13" x2="8" y2="13"></line>
                        <line x1="16" y1="17" x2="8" y2="17"></line>
                        <polyline points="10 9 9 9 8 9"></polyline>
                    </svg>
                </div>
                <div class="view-news-btn" onclick="goToAnalysisPage(${item.id})" title="数据分析">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M18 20V10"></path>
                        <path d="M12 20V4"></path>
                        <path d="M6 20v-6"></path>
                    </svg>
                </div>
                ` : ''}
                <div class="delete-btn" onclick="deleteHistoryItem(${item.id}, event)">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                </div>
            </div>
        </li>`;
    }).join('');
}

async function loadHistoryItem(url) {
    // 检查URL是否为空
    if (!url || url === 'undefined' || url === 'null') {
        showView(resultView);
        displayErrorResult('该历史记录没有关联的URL，可能是批量爬取记录。请使用"查看关联新闻"按钮查看详情。');
        return;
    }
    
    showView(resultView);
    articleWrapper.innerHTML = '<div class="loading-container"><span><div class="loading"></div>正在获取历史记录...</span></div>';
    
    try {
        // 确保URL安全编码，处理可能的双重编码问题
        const safeUrl = encodeURIComponent(decodeURIComponent(url));
        // 使用/api/news/detail接口获取已爬取的新闻数据
        const response = await fetch(`/api/news/detail?url=${safeUrl}`);
        if (response.ok) {
            const newsData = await response.json();
            displaySuccessResult(newsData);
        } else if (response.status === 404) {
            displayErrorResult('无法获取历史记录详情，该新闻可能已被删除或URL格式不正确');
        } else {
            displayErrorResult(`获取历史记录失败：${response.status} ${response.statusText}`);
        }
    } catch (error) {
        console.error('获取历史记录详情错误:', error);
        displayErrorResult('获取历史记录详情时发生错误：' + error.message);
    }
}

async function deleteHistoryItem(id, event) {
    event.stopPropagation();
    if (!confirm('确定要删除这条历史记录吗？')) return;
    
    try {
        const response = await fetch(`/api/history/${id}`, { method: 'DELETE' });
        if (response.ok) {
            const item = document.querySelector(`.history-item[data-id="${id}"]`);
            if (item) item.remove();
            if (historyList.children.length === 0) {
                historyList.innerHTML = '<li style="padding: 10px; color: var(--text-color-secondary);">暂无历史记录</li>';
            }
        } else {
            const errorData = await response.json();
            alert(`删除失败: ${errorData.message || '未知错误'}`);
        }
    } catch (error) {
        alert('删除历史记录时发生错误');
    }
}

getEl('clearAllHistory').addEventListener('click', async () => {
    if (!confirm('确定要清空所有历史记录吗？此操作不可恢复！')) return;
    
    try {
        const response = await fetch('/api/history/all', { method: 'DELETE' });
        if (response.ok) {
            const data = await response.json();
            historyList.innerHTML = '<li style="padding: 10px; color: var(--text-color-secondary);">暂无历史记录</li>';
            alert(`已清空所有历史记录，共删除 ${data.deletedCount} 条记录`);
        } else {
            const errorData = await response.json();
            alert(`清空失败: ${errorData.message || '未知错误'}`);
        }
    } catch (error) {
        alert('清空历史记录时发生错误');
    }
});

// ----------------- 收藏夹 (LocalStorage) -----------------
let favorites = JSON.parse(localStorage.getItem('favorites')) || [];

function updateFavoritesDisplay() {
    if (favorites.length === 0) {
        favoritesList.innerHTML = '<div style="text-align:center; padding: 20px 0; color: var(--text-color-secondary);">暂无收藏</div>';
    } else {
        favoritesList.innerHTML = favorites.map((item, index) => `
            <li class="favorite-item">
                <a href="${item.url}" target="_blank" class="favorite-url" title="${item.url}">${item.url}</a>
                <span class="favorite-btn" onclick="deleteFavorite(${index})">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                </span>
            </li>`).join('');
    }
}

function addFavorite(url) {
    if (!url || !url.startsWith('http')) {
        alert('请输入有效的网址！');
        return;
    }
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
    addFavorite(url);
    if(url && url.startsWith('http')) hideDialog(favoriteDialog);
});

favoriteUrlInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') saveFavorite.click();
});

// ----------------- 主题切换 -----------------
const themeToggle = getEl('themeToggle');
const html = document.documentElement;

const savedTheme = localStorage.getItem('theme') || 'dark';
html.setAttribute('data-theme', savedTheme);

function updateThemeIcon() {
    const isDark = html.getAttribute('data-theme') === 'dark';
    themeToggle.innerHTML = isDark 
        ? `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"></path></svg>`
        : `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>`;
}

themeToggle.addEventListener('click', () => {
    const newTheme = html.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
    html.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);
    updateThemeIcon();
});

// ----------------- 初始化 -----------------
document.addEventListener('DOMContentLoaded', () => {
    updateThemeIcon();
    checkLoginStatusAndLoadData();
    updateFavoritesDisplay();
    updateInputForCrawlType();
    showView(homeView); // 初始显示主页
});

async function loadNewsByHistoryId(historyId, event) {
    if (event) event.stopPropagation();
    
    // 如果historyId为null，显示错误信息并返回
    if (historyId === null || historyId === 'null') {
        displayErrorResult('无法获取关联的爬取历史ID');
        return;
    }
    
    showView(resultView);
    articleWrapper.innerHTML = '<div class="loading-container"><span><div class="loading"></div>正在获取关联新闻数据...</span></div>';
    
    try {
        const response = await fetch(`/api/history/${historyId}/news`);
        if (response.ok) {
            const newsDataList = await response.json();
            
            if (newsDataList && newsDataList.length > 0) {
                // 显示批量新闻列表
                const resultHtml = `
                    <div class="batch-result-summary">
                        <h2 class="batch-result-title">爬取历史关联新闻</h2>
                        <div class="batch-result-info">共找到 <strong>${newsDataList.length}</strong> 条关联新闻</div>
                    </div>
                    <div class="news-list">
                        ${newsDataList.map((news, index) => `
                            <div class="news-item" onclick="displayNewsDetail(${JSON.stringify(news).replace(/"/g, '&quot;')})">
                                <div class="news-number">${index + 1}</div>
                                <div class="news-content">
                                    <h3 class="news-title">${news.title || '无标题'}</h3>
                                    <div class="news-meta">
                                        ${news.source ? `<span class="news-source">${news.source}</span>` : ''}
                                        ${news.publishTime ? `<span class="news-time">${new Date(news.publishTime).toLocaleString()}</span>` : ''}
                                    </div>
                                </div>
                            </div>
                        `).join('')}
                    </div>
                `;
                articleWrapper.innerHTML = resultHtml;
            } else {
                displayErrorResult('该爬取历史没有关联的新闻数据');
            }
        } else if (response.status === 404) {
            displayErrorResult('未找到该爬取历史记录');
        } else {
            displayErrorResult(`获取关联新闻失败：${response.status} ${response.statusText}`);
        }
    } catch (error) {
        console.error('获取关联新闻错误:', error);
        displayErrorResult('获取关联新闻时发生错误：' + error.message);
    }
}

function displayNewsDetail(newsData) {
    currentArticleData = newsData; // 缓存数据用于导出
    const resultHtml = `
        <header class="article-header">
            <h1>${newsData.title || '无标题'}</h1>
            <div class="article-meta">
                ${newsData.source ? `<div class="meta-item"><strong>来源:</strong> <span>${newsData.source}</span></div>` : ''}
                ${newsData.author ? `<div class="meta-item"><strong>作者:</strong> <span>${newsData.author}</span></div>` : ''}
                ${newsData.publishTime ? `<div class="meta-item"><strong>发布时间:</strong> <span>${new Date(newsData.publishTime).toLocaleString()}</span></div>` : ''}
                ${newsData.keywords ? `<div class="meta-item"><strong>关键词:</strong> <span>${newsData.keywords}</span></div>` : ''}
            </div>
        </header>
        <div class="article-body">
            ${newsData.content || '<p>无正文内容。</p>'}
        </div>
        <div class="article-actions">
            <button class="btn btn-secondary" onclick="loadNewsByHistoryId(${newsData.crawlHistoryId || 'null'})">返回列表</button>
            <button class="btn btn-primary" onclick="showDialog(saveDialog)">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 5px;">
                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                    <polyline points="17 21 17 13 7 13 7 21"></polyline>
                    <polyline points="7 3 7 8 15 8"></polyline>
                </svg>
                导出文档
            </button>
            ${newsData.crawlHistoryId ? `
            <button class="btn btn-secondary" onclick="goToAnalysisPage(${newsData.crawlHistoryId})">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 5px;">
                    <path d="M18 20V10"></path>
                    <path d="M12 20V4"></path>
                    <path d="M6 20v-6"></path>
                </svg>
                数据分析
            </button>
            ` : ''}
        </div>
    `;
    articleWrapper.innerHTML = resultHtml;
}

/**
 * 导出文档
 * @param {Object} params 导出参数
 */
function exportDocument(params) {
    const overlay = document.getElementById('overlay');
    const saveDialog = document.getElementById('saveDialog');
    
    // 显示加载状态
    const loadingElement = document.createElement('div');
    loadingElement.className = 'loading-overlay';
    loadingElement.innerHTML = `
        <div class="loading-spinner"></div>
        <div class="loading-text">正在生成文档，请稍候...</div>
    `;
    document.body.appendChild(loadingElement);
    
    // 关闭导出对话框
    if (saveDialog && overlay) {
        saveDialog.classList.remove('active');
        overlay.classList.remove('active');
    }
    
    // 发送导出请求
    fetch('/api/export', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(params)
    })
    .then(response => {
        // 移除加载状态
        document.body.removeChild(loadingElement);
        
        if (response.ok) {
            // 检查Content-Type
            const contentType = response.headers.get('Content-Type');
            if (contentType && (contentType.includes('application/pdf') || contentType.includes('application/vnd.openxmlformats-officedocument.wordprocessingml.document'))) {
                // 是文件下载
                return response.blob().then(blob => {
                    // 创建下载链接
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.style.display = 'none';
                    a.href = url;
                    
                    // 设置文件名
                    const filename = response.headers.get('Content-Disposition')
                        ? response.headers.get('Content-Disposition').split('filename=')[1].replace(/"/g, '')
                        : params.format === 'pdf' ? '导出文档.pdf' : '导出文档.docx';
                    
                    a.download = decodeURIComponent(filename);
                    document.body.appendChild(a);
                    a.click();
                    
                    // 清理
                    window.URL.revokeObjectURL(url);
                    document.body.removeChild(a);
                });
            } else {
                // 是JSON响应
                return response.json().then(data => {
                    if (data.error) {
                        alert('导出失败: ' + data.error);
                    } else if (data.message) {
                        alert(data.message);
                    } else {
                        alert('导出成功');
                    }
                });
            }
        } else {
            // 处理错误响应
            return response.text().then(text => {
                alert('导出失败: ' + text);
            });
        }
    })
    .catch(error => {
        // 移除加载状态
        if (document.body.contains(loadingElement)) {
            document.body.removeChild(loadingElement);
        }
        console.error('导出错误:', error);
        alert('导出过程中发生错误: ' + error.message);
    });
}

// 初始化字体预览功能
document.addEventListener('DOMContentLoaded', function() {
    // 字体选择预览功能
    const fontFamilySelect = document.getElementById('fontFamily');
    if (fontFamilySelect) {
        // 创建预览元素
        const previewContainer = document.createElement('div');
        previewContainer.className = 'font-preview-container';
        previewContainer.innerHTML = `
            <div class="font-preview-label">预览:</div>
            <div class="font-preview-text">这是字体预览文本 - 网页爬虫系统</div>
        `;
        
        // 将预览元素插入到字体选择下拉框后面
        fontFamilySelect.parentNode.appendChild(previewContainer);
        
        // 获取预览文本元素
        const previewText = previewContainer.querySelector('.font-preview-text');
        
        // 初始化预览
        if (previewText) {
            previewText.style.fontFamily = fontFamilySelect.value;
        }
        
        // 监听字体选择变化
        fontFamilySelect.addEventListener('change', function() {
            if (previewText) {
                previewText.style.fontFamily = this.value;
                console.log('字体已更改为:', this.value);
            }
        });
    }
});