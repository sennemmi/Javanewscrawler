/**
 * 数据分析功能 JavaScript
 */

// 全局变量
let currentHistoryId = null; // 当前分析的历史记录ID
let wordCloudData = null; // 词云数据
let hotWordsData = null; // 热词数据
let sourceDistributionData = null; // 来源分布数据

// 常量
const MAX_HOT_WORDS = 10; // 热词排行显示数量
const DEFAULT_KEYWORD = "经济"; // 默认关键词

// DOM元素
const getEl = (id) => document.getElementById(id);
const analysisView = getEl('analysisView');
const analysisBackBtn = getEl('analysisBackBtn');
const resultView = getEl('resultView');
const homeView = getEl('homeView');
const wordCloudContainer = getEl('wordCloudContainer');
const hotWordsList = getEl('hotWordsList');
const trendChartContainer = getEl('trendChartContainer');
const sourceChartContainer = getEl('sourceChartContainer');
const trendKeywordInput = getEl('trendKeywordInput');
const trendSearchBtn = getEl('trendSearchBtn');
const analysisType = getEl('analysisType');
const analysisTabs = document.querySelectorAll('.analysis-tab');
const analysisCards = document.querySelectorAll('.analysis-card');

// 初始化
document.addEventListener('DOMContentLoaded', () => {
    // 绑定事件
    analysisBackBtn.addEventListener('click', () => {
        // 返回结果页面
        analysisView.style.display = 'none';
        resultView.style.display = 'block';
    });

    // 分析类型选择
    analysisType.addEventListener('change', (e) => {
        const selectedType = e.target.value;
        filterAnalysisCards(selectedType);
    });

    // 标签切换
    analysisTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            // 更新激活的标签
            analysisTabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            
            // 筛选卡片
            const tabType = tab.getAttribute('data-tab');
            filterAnalysisCards(tabType);
        });
    });

    // 趋势搜索按钮
    trendSearchBtn.addEventListener('click', () => {
        const keyword = trendKeywordInput.value.trim();
        if (keyword && currentHistoryId) {
            loadKeywordTimeTrend(keyword);
        } else {
            alert('请输入关键词');
        }
    });

    trendKeywordInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') trendSearchBtn.click();
    });
});

/**
 * 根据类型筛选分析卡片
 * @param {string} type 分析类型
 */
function filterAnalysisCards(type) {
    if (type === 'all') {
        analysisCards.forEach(card => card.style.display = 'block');
    } else {
        analysisCards.forEach(card => {
            if (card.getAttribute('data-type') === type) {
                card.style.display = 'block';
            } else {
                card.style.display = 'none';
            }
        });
    }
}

/**
 * 启动数据分析，加载所有分析数据
 * @param {number} historyId 历史记录ID
 */
function startAnalysis(historyId) {
    if (!historyId) {
        alert('无法获取历史记录ID，无法进行分析');
        return;
    }

    currentHistoryId = historyId;
    showView(analysisView);
    
    // 初始化默认显示所有卡片
    filterAnalysisCards('all');
    
    // 加载词云数据
    loadWordCloudData();
    
    // 加载热词排行
    loadHotWordsData();
    
    // 加载默认关键词趋势
    trendKeywordInput.value = DEFAULT_KEYWORD;
    loadKeywordTimeTrend(DEFAULT_KEYWORD);
    
    // 加载来源分布
    loadSourceDistribution();
}

/**
 * 加载词云数据
 */
async function loadWordCloudData() {
    if (!currentHistoryId) return;
    
    showLoading(wordCloudContainer);
    
    try {
        const response = await fetch('/api/analysis/word-cloud', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ 
                historyId: currentHistoryId,
                limit: 100
            })
        });
        
        if (response.ok) {
            const data = await response.json();
            wordCloudData = data.wordCloud;
            
            if (wordCloudData && wordCloudData.length > 0) {
                renderWordCloud();
            } else {
                showEmptyState(wordCloudContainer, '没有足够的数据生成词云');
            }
        } else {
            const error = await response.text();
            showError(wordCloudContainer, `加载词云数据失败: ${error}`);
        }
    } catch (error) {
        console.error('加载词云错误:', error);
        showError(wordCloudContainer, '加载词云数据时发生错误');
    }
}

/**
 * 渲染词云
 */
function renderWordCloud() {
    if (!wordCloudData || wordCloudData.length === 0) return;
    
    // 清空容器
    wordCloudContainer.innerHTML = '';
    
    // 转换数据格式
    const list = wordCloudData.map(item => [item.text, item.weight]);
    
    // 渲染词云
    WordCloud(wordCloudContainer, {
        list: list,
        gridSize: 16,
        weightFactor: 10,
        fontFamily: 'Microsoft YaHei, sans-serif',
        color: function(word, weight) {
            // 根据权重设置颜色
            const hue = 210 + Math.floor(weight / 100 * 90);
            return `hsl(${hue}, 70%, 60%)`;
        },
        hover: function(item) {
            if (item) {
                // 可选: 添加鼠标悬停效果
            }
        },
        click: function(item) {
            if (item) {
                // 点击词语时，自动填充到趋势搜索框并触发搜索
                trendKeywordInput.value = item[0];
                trendSearchBtn.click();
                
                // 自动切换到趋势标签
                analysisTabs.forEach(tab => {
                    if (tab.getAttribute('data-tab') === 'timeTrend') {
                        tab.click();
                    }
                });
            }
        }
    });
}

/**
 * 加载热词排行数据
 */
async function loadHotWordsData() {
    if (!currentHistoryId) return;
    
    showLoading(hotWordsList);
    
    try {
        const response = await fetch('/api/analysis/hot-words', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ 
                historyId: currentHistoryId,
                limit: MAX_HOT_WORDS
            })
        });
        
        if (response.ok) {
            const data = await response.json();
            hotWordsData = data.hotWords;
            
            if (hotWordsData && hotWordsData.length > 0) {
                renderHotWordsList();
            } else {
                showEmptyState(hotWordsList, '没有足够的数据生成热词排行');
            }
        } else {
            const error = await response.text();
            showError(hotWordsList, `加载热词数据失败: ${error}`);
        }
    } catch (error) {
        console.error('加载热词错误:', error);
        showError(hotWordsList, '加载热词数据时发生错误');
    }
}

/**
 * 渲染热词排行榜
 */
function renderHotWordsList() {
    if (!hotWordsData || hotWordsData.length === 0) return;
    
    // 清空容器
    hotWordsList.innerHTML = '';
    
    // 渲染列表
    hotWordsData.forEach((item, index) => {
        const rankClass = index < 3 ? `top-${index + 1}` : '';
        
        const itemHtml = `
            <div class="hot-word-item" data-keyword="${item.word}">
                <div class="hot-word-rank">
                    <div class="hot-word-number ${rankClass}">${index + 1}</div>
                    <div class="hot-word-text">${item.word}</div>
                </div>
                <div class="hot-word-count">${item.count}</div>
            </div>
        `;
        
        hotWordsList.innerHTML += itemHtml;
    });
    
    // 绑定点击事件
    document.querySelectorAll('.hot-word-item').forEach(item => {
        item.addEventListener('click', () => {
            const keyword = item.getAttribute('data-keyword');
            if (keyword) {
                // 点击热词时，自动填充到趋势搜索框并触发搜索
                trendKeywordInput.value = keyword;
                trendSearchBtn.click();
                
                // 自动切换到趋势标签
                analysisTabs.forEach(tab => {
                    if (tab.getAttribute('data-tab') === 'timeTrend') {
                        tab.click();
                    }
                });
            }
        });
    });
}

/**
 * 加载关键词时间趋势
 * @param {string} keyword 关键词
 */
async function loadKeywordTimeTrend(keyword) {
    if (!currentHistoryId || !keyword) return;
    
    showLoading(trendChartContainer);
    
    try {
        const response = await fetch('/api/analysis/time-trend', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ 
                historyId: currentHistoryId,
                keyword: keyword,
                timeUnit: 'day'
            })
        });
        
        if (response.ok) {
            const data = await response.json();
            
            if (data.trendData && data.trendData.length > 0) {
                renderTrendChart(data);
            } else {
                showEmptyState(trendChartContainer, `没有找到关键词"${keyword}"的趋势数据`);
            }
        } else {
            const error = await response.text();
            showError(trendChartContainer, `加载趋势数据失败: ${error}`);
        }
    } catch (error) {
        console.error('加载趋势错误:', error);
        showError(trendChartContainer, '加载趋势数据时发生错误');
    }
}

/**
 * 渲染趋势图
 * @param {Object} data 趋势数据
 */
function renderTrendChart(data) {
    if (!data || !data.trendData || data.trendData.length === 0) return;
    
    // 准备数据
    const timePoints = data.trendData.map(item => item.timePoint);
    const counts = data.trendData.map(item => item.count);
    
    // 初始化echarts
    const chartDom = trendChartContainer;
    const myChart = echarts.init(chartDom, null, { renderer: 'svg' });
    
    // 配置项
    const option = {
        title: {
            text: `关键词"${data.keyword}"时间趋势`,
            textStyle: {
                color: getComputedStyle(document.documentElement).getPropertyValue('--text-color').trim()
            }
        },
        tooltip: {
            trigger: 'axis'
        },
        xAxis: {
            type: 'category',
            data: timePoints,
            axisLine: {
                lineStyle: {
                    color: getComputedStyle(document.documentElement).getPropertyValue('--border-color').trim()
                }
            },
            axisLabel: {
                color: getComputedStyle(document.documentElement).getPropertyValue('--text-color-secondary').trim()
            }
        },
        yAxis: {
            type: 'value',
            name: '出现次数',
            nameTextStyle: {
                color: getComputedStyle(document.documentElement).getPropertyValue('--text-color-secondary').trim()
            },
            axisLine: {
                lineStyle: {
                    color: getComputedStyle(document.documentElement).getPropertyValue('--border-color').trim()
                }
            },
            axisLabel: {
                color: getComputedStyle(document.documentElement).getPropertyValue('--text-color-secondary').trim()
            },
            splitLine: {
                lineStyle: {
                    color: getComputedStyle(document.documentElement).getPropertyValue('--border-color').trim(),
                    opacity: 0.3
                }
            }
        },
        series: [{
            name: data.keyword,
            type: 'line',
            data: counts,
            smooth: true,
            symbolSize: 8,
            itemStyle: {
                color: getComputedStyle(document.documentElement).getPropertyValue('--active-color').trim()
            },
            areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    { offset: 0, color: getComputedStyle(document.documentElement).getPropertyValue('--active-color').trim() + 'CC' },
                    { offset: 1, color: getComputedStyle(document.documentElement).getPropertyValue('--active-color').trim() + '33' }
                ])
            }
        }]
    };
    
    // 渲染图表
    myChart.setOption(option);
    
    // 响应窗口大小变化
    window.addEventListener('resize', function() {
        myChart.resize();
    });
}

/**
 * 加载来源分布数据
 */
async function loadSourceDistribution() {
    if (!currentHistoryId) return;
    
    showLoading(sourceChartContainer);
    
    try {
        const response = await fetch('/api/analysis/source-distribution', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ 
                historyId: currentHistoryId
            })
        });
        
        if (response.ok) {
            const data = await response.json();
            sourceDistributionData = data.sourceDistribution;
            
            if (sourceDistributionData && sourceDistributionData.length > 0) {
                renderSourceDistribution();
            } else {
                showEmptyState(sourceChartContainer, '没有足够的数据生成来源分布');
            }
        } else {
            const error = await response.text();
            showError(sourceChartContainer, `加载来源分布数据失败: ${error}`);
        }
    } catch (error) {
        console.error('加载来源分布错误:', error);
        showError(sourceChartContainer, '加载来源分布数据时发生错误');
    }
}

/**
 * 渲染来源分布图
 */
function renderSourceDistribution() {
    if (!sourceDistributionData || sourceDistributionData.length === 0) return;
    
    // 准备数据
    const chartData = sourceDistributionData.map(item => ({
        name: item.source,
        value: item.count
    }));
    
    // 初始化echarts
    const chartDom = sourceChartContainer;
    const myChart = echarts.init(chartDom, null, { renderer: 'svg' });
    
    // 配置项
    const option = {
        title: {
            text: '新闻来源分布',
            left: 'center',
            textStyle: {
                color: getComputedStyle(document.documentElement).getPropertyValue('--text-color').trim()
            }
        },
        tooltip: {
            trigger: 'item',
            formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
            orient: 'vertical',
            left: 'left',
            textStyle: {
                color: getComputedStyle(document.documentElement).getPropertyValue('--text-color-secondary').trim()
            }
        },
        series: [
            {
                name: '来源分布',
                type: 'pie',
                radius: '60%',
                center: ['50%', '60%'],
                data: chartData,
                label: {
                    color: getComputedStyle(document.documentElement).getPropertyValue('--text-color').trim()
                },
                emphasis: {
                    itemStyle: {
                        shadowBlur: 10,
                        shadowOffsetX: 0,
                        shadowColor: 'rgba(0, 0, 0, 0.5)'
                    }
                }
            }
        ]
    };
    
    // 渲染图表
    myChart.setOption(option);
    
    // 响应窗口大小变化
    window.addEventListener('resize', function() {
        myChart.resize();
    });
}

/**
 * 显示加载中状态
 * @param {HTMLElement} container 容器元素
 */
function showLoading(container) {
    container.innerHTML = `
        <div class="loading-overlay">
            <div class="analysis-loading"></div>
        </div>
    `;
}

/**
 * 显示错误状态
 * @param {HTMLElement} container 容器元素
 * @param {string} message 错误信息
 */
function showError(container, message) {
    container.innerHTML = `
        <div class="empty-state">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <div class="empty-state-title">加载失败</div>
            <div class="empty-state-desc">${message}</div>
        </div>
    `;
}

/**
 * 显示空状态
 * @param {HTMLElement} container 容器元素
 * @param {string} message 提示信息
 */
function showEmptyState(container, message) {
    container.innerHTML = `
        <div class="empty-state">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                <polyline points="7 10 12 15 17 10"></polyline>
                <line x1="12" y1="15" x2="12" y2="3"></line>
            </svg>
            <div class="empty-state-title">暂无数据</div>
            <div class="empty-state-desc">${message}</div>
        </div>
    `;
}

/**
 * 切换视图
 * @param {HTMLElement} view 要显示的视图
 */
function showView(view) {
    // 隐藏所有视图
    homeView.style.display = 'none';
    resultView.style.display = 'none';
    analysisView.style.display = 'none';
    
    // 显示指定视图
    view.style.display = 'block';
}

// 导出接口，提供给外部调用
window.dataAnalysis = {
    startAnalysis: startAnalysis
}; 