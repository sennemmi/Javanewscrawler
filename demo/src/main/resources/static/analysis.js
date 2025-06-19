/**
 * 数据分析功能 JavaScript
 */

// 全局变量
let analysis_currentHistoryId = null; // 当前分析的历史记录ID
let analysis_wordCloudData = null; // 词云数据
let analysis_hotWordsData = null; // 热词数据
let analysis_sourceDistributionData = null; // 来源分布数据
let analysis_view, analysis_backBtn, analysis_resultView, analysis_homeView, analysis_wordCloudContainer, analysis_hotWordsList;
let analysis_trendChartContainer, analysis_sourceChartContainer, analysis_trendKeywordInput, analysis_trendSearchBtn, analysis_type;
let analysis_tabs, analysis_topCards, analysis_timeUnitRadios;

// 常量
const ANALYSIS_MAX_HOT_WORDS = 10; // 热词排行显示数量
const ANALYSIS_DEFAULT_KEYWORD = "经济"; // 默认关键词

// 获取DOM元素的辅助函数
const analysis_getEl = (id) => document.getElementById(id);

/**
 * 启动数据分析，加载所有分析数据
 * @param {number} historyId 历史记录ID
 */
function analysis_startAnalysis(historyId) {
    if (!historyId) {
        alert('无法获取历史记录ID，无法进行分析');
        return;
    }
    
    console.log('开始数据分析，historyId =', historyId);
    
    // 确保DOM元素已初始化
    analysis_initDOMElements();

    analysis_currentHistoryId = historyId;
    
    // 直接设置显示分析视图，隐藏其他视图
    const homeView = document.getElementById('homeView');
    const resultView = document.getElementById('resultView');
    
    if (homeView) homeView.style.display = 'none';
    if (resultView) resultView.style.display = 'none';
    if (analysis_view) analysis_view.style.display = 'block';
    
    // 移除了初始化默认显示所有卡片的调用，因为现在始终显示所有卡片
    
    console.log('开始加载词云数据');
    // 加载词云数据
    analysis_loadWordCloudData();
    
    console.log('开始加载热词排行');
    // 加载热词排行
    analysis_loadHotWordsData();
    
    // 先直接渲染一个默认的趋势图，确保用户体验
    analysis_renderDefaultTrendChart();
    
    // 加载默认关键词趋势
    if (analysis_trendKeywordInput) {
        analysis_trendKeywordInput.value = ANALYSIS_DEFAULT_KEYWORD;
        console.log('设置默认关键词:', ANALYSIS_DEFAULT_KEYWORD);
        // 获取当前选择的时间单位
        const selectedTimeUnit = document.querySelector('input[name="timeUnit"]:checked');
        const timeUnit = selectedTimeUnit ? selectedTimeUnit.value : 'day';
        console.log('当前选择的时间单位:', timeUnit);
        analysis_loadKeywordTimeTrend(ANALYSIS_DEFAULT_KEYWORD, timeUnit);
    }
    
    console.log('开始加载来源分布');
    // 加载来源分布
    analysis_loadSourceDistribution();
    
    // 添加调试代码 - 如果10秒后内容来源分布还在加载中，使用模拟数据
    setTimeout(() => {
        if (analysis_sourceChartContainer && analysis_sourceChartContainer.innerHTML.includes('loading-overlay')) {
            console.log('来源分布加载超时，使用模拟数据');
            // 模拟数据
            analysis_sourceDistributionData = [
                { source: "新浪新闻", count: 42 },
                { source: "人民日报", count: 28 },
                { source: "央视网", count: 16 },
                { source: "环球时报", count: 9 },
                { source: "中国日报", count: 5 }
            ];
            analysis_renderSourceDistribution();
        }
    }, 10000);
    
    // 添加调试代码 - 检查时间趋势图是否加载
    setTimeout(() => {
        if (analysis_trendChartContainer && analysis_trendChartContainer.innerHTML.includes('loading-overlay')) {
            console.log('时间趋势图加载超时，使用模拟数据');
            analysis_renderDefaultTrendChart();
        }
    }, 5000);
}

/**
 * 初始化DOM元素
 */
function analysis_initDOMElements() {
    analysis_view = analysis_getEl('analysisView');
    analysis_backBtn = analysis_getEl('analysisBackBtn');
    analysis_resultView = analysis_getEl('resultView');
    analysis_homeView = analysis_getEl('homeView');
    analysis_wordCloudContainer = analysis_getEl('wordCloudContainer');
    analysis_hotWordsList = analysis_getEl('hotWordsList');
    analysis_trendChartContainer = analysis_getEl('trendChartContainer');
    analysis_sourceChartContainer = analysis_getEl('sourceChartContainer');
    analysis_trendKeywordInput = analysis_getEl('trendKeywordInput');
    analysis_trendSearchBtn = analysis_getEl('trendSearchBtn');
    analysis_topCards = document.querySelectorAll('.analysis-card');
    analysis_timeUnitRadios = document.querySelectorAll('input[name="timeUnit"]');
    
    // 确保元素存在
    if (!analysis_view || !analysis_resultView || !analysis_wordCloudContainer) {
        console.error('无法找到必要的DOM元素，数据分析功能可能无法正常工作');
    }
}

// 初始化
document.addEventListener('DOMContentLoaded', () => {
    // 初始化DOM元素
    analysis_initDOMElements();
    
    // 绑定事件
    if (analysis_backBtn) {
        analysis_backBtn.addEventListener('click', () => {
            // 返回结果页面
            if (analysis_view) analysis_view.style.display = 'none';
            if (analysis_resultView) analysis_resultView.style.display = 'block';
        });
    }

    // 趋势搜索按钮
    if (analysis_trendSearchBtn) {
        analysis_trendSearchBtn.addEventListener('click', () => {
            const keyword = analysis_trendKeywordInput.value.trim();
            if (keyword && analysis_currentHistoryId) {
                const selectedTimeUnit = document.querySelector('input[name="timeUnit"]:checked');
                const timeUnit = selectedTimeUnit ? selectedTimeUnit.value : 'day';
                analysis_loadKeywordTimeTrend(keyword, timeUnit);
            } else {
                alert('请输入关键词');
            }
        });
    }

    if (analysis_trendKeywordInput) {
        analysis_trendKeywordInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && analysis_trendSearchBtn) analysis_trendSearchBtn.click();
        });
    }

    // 时间单位选择
    if (analysis_timeUnitRadios) {
        analysis_timeUnitRadios.forEach(radio => {
            radio.addEventListener('change', () => {
                const selectedTimeUnit = radio.value;
                if (analysis_currentHistoryId) {
                    analysis_loadKeywordTimeTrend(analysis_trendKeywordInput.value, selectedTimeUnit);
                }
            });
        });
    }

    // 绑定点击事件
    document.querySelectorAll('.hot-word-item').forEach(item => {
        item.addEventListener('click', () => {
            const keyword = item.getAttribute('data-keyword');
            if (keyword) {
                // 点击热词时，自动填充到趋势搜索框并触发搜索
                analysis_trendKeywordInput.value = keyword;
                analysis_trendSearchBtn.click();
                
                // 自动滚动到趋势图区域
                document.querySelector('.trend-section').scrollIntoView({ behavior: 'smooth' });
            }
        });
    });
});

/**
 * 根据类型筛选分析卡片
 * @param {string} type 分析类型
 */
/* function analysis_filterAnalysisCards(type) {
    if (type === 'all') {
        analysis_topCards.forEach(card => card.style.display = 'block');
    } else {
        analysis_topCards.forEach(card => {
            if (card.getAttribute('data-type') === type) {
                card.style.display = 'block';
            } else {
                card.style.display = 'none';
            }
        });
    }
} */

/**
 * 加载词云数据
 */
async function analysis_loadWordCloudData() {
    if (!analysis_currentHistoryId) return;
    
    analysis_showLoading(analysis_wordCloudContainer);
    
    try {
        const response = await fetch('/api/analysis/word-cloud', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ 
                historyId: analysis_currentHistoryId,
                limit: 100
            })
        });
        
        if (response.ok) {
            const data = await response.json();
            analysis_wordCloudData = data.wordCloud;
            
            if (analysis_wordCloudData && analysis_wordCloudData.length > 0) {
                analysis_renderWordCloud();
            } else {
                analysis_showEmptyState(analysis_wordCloudContainer, '没有足够的数据生成词云');
            }
        } else {
            const error = await response.text();
            analysis_showError(analysis_wordCloudContainer, `加载词云数据失败: ${error}`);
        }
    } catch (error) {
        console.error('加载词云错误:', error);
        analysis_showError(analysis_wordCloudContainer, '加载词云数据时发生错误');
    }
}

/**
 * 渲染词云
 */
function analysis_renderWordCloud() {
    if (!analysis_wordCloudData || analysis_wordCloudData.length === 0) {
        console.error('词云数据为空，无法渲染');
        analysis_showEmptyState(analysis_wordCloudContainer, '没有足够的数据生成词云');
        return;
    }
    
    console.log('开始渲染词云，数据长度:', analysis_wordCloudData.length);
    
    // 确保词云容器存在
    if (!analysis_wordCloudContainer) {
        analysis_wordCloudContainer = document.getElementById('wordCloudContainer');
        if (!analysis_wordCloudContainer) {
            console.error('词云容器不存在，无法渲染');
            return;
        }
    }
    
    // 清空容器（包括加载动画）
    analysis_wordCloudContainer.innerHTML = '';
    
    // 确保WordCloud库已加载
    if (typeof WordCloud === 'undefined') {
        console.error('WordCloud库未加载，尝试加载');
        const script = document.createElement('script');
        script.src = 'https://cdn.jsdelivr.net/npm/wordcloud@1.2.2/src/wordcloud2.min.js';
        script.onload = function() {
            console.log('WordCloud库加载成功，重新尝试渲染');
            renderCloud();
        };
        script.onerror = function() {
            console.error('WordCloud库加载失败');
            analysis_showError(analysis_wordCloudContainer, '无法加载词云库，请检查网络连接');
        };
        document.head.appendChild(script);
        return;
    }
    
    // 渲染词云
    renderCloud();
    
    function renderCloud() {
        try {
            // 转换数据格式
            const list = analysis_wordCloudData.map(item => [item.text, item.weight]);
            console.log('词云数据处理完成，开始渲染');
            
            // 渲染词云
            WordCloud(analysis_wordCloudContainer, {
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
                        console.log(`词云点击: ${item[0]}`);
                        // 点击词语时，自动填充到趋势搜索框并触发搜索
                        if (analysis_trendKeywordInput && analysis_trendSearchBtn) {
                            analysis_trendKeywordInput.value = item[0];
                            analysis_trendSearchBtn.click();
                            
                            // 自动滚动到趋势图区域
                            const trendSection = document.querySelector('.trend-section');
                            if (trendSection) {
                                trendSection.scrollIntoView({ behavior: 'smooth' });
                            }
                        } else {
                            console.error('趋势输入框或搜索按钮不存在');
                            // 手动触发趋势加载
                            const selectedTimeUnit = document.querySelector('input[name="timeUnit"]:checked');
                            const timeUnit = selectedTimeUnit ? selectedTimeUnit.value : 'day';
                            analysis_loadKeywordTimeTrend(item[0], timeUnit);
                        }
                    }
                }
            });
            
            console.log('词云渲染成功');
        } catch (error) {
            console.error('词云渲染失败:', error);
            analysis_showError(analysis_wordCloudContainer, '渲染词云失败: ' + error.message);
        }
    }
}

/**
 * 加载热词排行数据
 */
async function analysis_loadHotWordsData() {
    if (!analysis_currentHistoryId) return;
    
    analysis_showLoading(analysis_hotWordsList);
    
    try {
        const response = await fetch('/api/analysis/hot-words', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ 
                historyId: analysis_currentHistoryId,
                limit: ANALYSIS_MAX_HOT_WORDS
            })
        });
        
        if (response.ok) {
            const data = await response.json();
            analysis_hotWordsData = data.hotWords;
            
            if (analysis_hotWordsData && analysis_hotWordsData.length > 0) {
                analysis_renderHotWordsList();
            } else {
                analysis_showEmptyState(analysis_hotWordsList, '没有足够的数据生成热词排行');
            }
        } else {
            const error = await response.text();
            analysis_showError(analysis_hotWordsList, `加载热词数据失败: ${error}`);
        }
    } catch (error) {
        console.error('加载热词错误:', error);
        analysis_showError(analysis_hotWordsList, '加载热词数据时发生错误');
    }
}

/**
 * 渲染热词排行榜
 */
function analysis_renderHotWordsList() {
    if (!analysis_hotWordsData || analysis_hotWordsData.length === 0) {
        analysis_showEmptyState(analysis_hotWordsList, '没有足够的数据生成热词排行');
        return;
    }
    
    // 清空容器（包括加载动画）
    analysis_hotWordsList.innerHTML = '';
    
    // 渲染列表
    analysis_hotWordsData.forEach((item, index) => {
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
        
        analysis_hotWordsList.innerHTML += itemHtml;
    });
    
    // 绑定点击事件（在渲染完成后绑定）
    const hotWordItems = analysis_hotWordsList.querySelectorAll('.hot-word-item');
    console.log(`找到 ${hotWordItems.length} 个热词项，开始绑定点击事件`);
    
    hotWordItems.forEach(item => {
        item.addEventListener('click', () => {
            const keyword = item.getAttribute('data-keyword');
            if (keyword) {
                console.log(`热词点击: ${keyword}`);
                // 点击热词时，自动填充到趋势搜索框并触发搜索
                if (analysis_trendKeywordInput) {
                    analysis_trendKeywordInput.value = keyword;
                    if (analysis_trendSearchBtn) {
                        analysis_trendSearchBtn.click();
                    } else {
                        console.error('趋势搜索按钮不存在');
                        // 手动触发趋势加载
                        const selectedTimeUnit = document.querySelector('input[name="timeUnit"]:checked');
                        const timeUnit = selectedTimeUnit ? selectedTimeUnit.value : 'day';
                        analysis_loadKeywordTimeTrend(keyword, timeUnit);
                    }
                } else {
                    console.error('趋势关键词输入框不存在');
                }
                
                // 自动滚动到趋势图区域
                const trendSection = document.querySelector('.trend-section');
                if (trendSection) {
                    trendSection.scrollIntoView({ behavior: 'smooth' });
                } else {
                    console.error('趋势图区域不存在');
                }
            }
        });
    });
    
    console.log('热词排行榜渲染成功，已绑定点击事件');
}

/**
 * 加载关键词时间趋势
 * @param {string} keyword 关键词
 * @param {string} timeUnit 时间单位(day, hour6, hour12)
 */
async function analysis_loadKeywordTimeTrend(keyword, timeUnit = 'day') {
    if (!analysis_currentHistoryId || !keyword) return;
    
    console.log('开始加载关键词时间趋势:', keyword, '时间单位:', timeUnit);
    
    // 获取选中的时间单位
    if (!timeUnit) {
        const selectedTimeUnit = document.querySelector('input[name="timeUnit"]:checked');
        timeUnit = selectedTimeUnit ? selectedTimeUnit.value : 'day';
        console.log('从DOM获取到时间单位:', timeUnit);
    }
    
    analysis_showLoading(analysis_trendChartContainer);
    
    try {
        console.log('发送趋势请求，参数:', { historyId: analysis_currentHistoryId, keyword, timeUnit });
        
        const response = await fetch('/api/analysis/time-trend', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ 
                historyId: analysis_currentHistoryId,
                keyword: keyword,
                timeUnit: timeUnit
            })
        });
        
        console.log('趋势请求响应状态:', response.status);
        
        if (response.ok) {
            const data = await response.json();
            console.log('获取到趋势数据:', data);
            
            if (data.trendData && data.trendData.length > 0) {
                analysis_renderTrendChart(data);
            } else {
                console.log('没有找到趋势数据');
                analysis_showEmptyState(analysis_trendChartContainer, `没有找到关键词"${keyword}"的趋势数据`);
            }
        } else {
            const error = await response.text();
            console.error('加载趋势数据失败:', error);
            analysis_showError(analysis_trendChartContainer, `加载趋势数据失败: ${error}`);
        }
    } catch (error) {
        console.error('加载趋势错误:', error);
        analysis_showError(analysis_trendChartContainer, '加载趋势数据时发生错误');
    }
}

/**
 * 渲染趋势图
 * @param {Object} data 趋势数据
 */
function analysis_renderTrendChart(data) {
    if (!data || !data.trendData || data.trendData.length === 0) {
        console.error('趋势数据无效:', data);
        return;
    }
    
    console.log('开始渲染趋势图，数据:', data);
    
    // 确保趋势图容器存在
    if (!analysis_trendChartContainer) {
        analysis_trendChartContainer = document.getElementById('trendChartContainer');
        if (!analysis_trendChartContainer) {
            console.error('趋势图容器不存在，无法渲染');
            return;
        }
    }
    
    // 清空容器内容（包括加载动画）
    analysis_trendChartContainer.innerHTML = '';
    
    // 准备数据
    const timePoints = data.trendData.map(item => item.timePoint);
    const counts = data.trendData.map(item => item.count);
    
    console.log('趋势图时间点:', timePoints);
    console.log('趋势图数值:', counts);
    
    // 添加一个临时元素作为echarts容器
    const chartContainer = document.createElement('div');
    chartContainer.style.width = '100%';
    chartContainer.style.height = '300px';
    analysis_trendChartContainer.appendChild(chartContainer);
    
    // 确保echarts库已加载
    if (typeof echarts === 'undefined') {
        console.error('echarts库未加载，尝试加载');
        const script = document.createElement('script');
        script.src = 'https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js';
        script.onload = function() {
            console.log('echarts库加载成功，重新尝试渲染');
            initChart();
        };
        script.onerror = function() {
            console.error('echarts库加载失败');
            analysis_showError(analysis_trendChartContainer, '无法加载图表库，请检查网络连接');
        };
        document.head.appendChild(script);
        return;
    }
    
    // 初始化并渲染图表
    initChart();
    
    function initChart() {
        try {
            console.log('初始化echarts实例');
            const myChart = echarts.init(chartContainer);
            
            // 配置项
            const option = {
                title: {
                    text: `关键词"${data.keyword}"时间趋势 (${data.timeUnit === 'day' ? '日' : data.timeUnit === 'hour6' ? '6小时' : '12小时'})`,
                    textStyle: {
                        color: getComputedStyle(document.documentElement).getPropertyValue('--text-color').trim()
                    }
                },
                tooltip: {
                    trigger: 'axis'
                },
                grid: {
                    left: '3%',
                    right: '4%',
                    bottom: '3%',
                    containLabel: true
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
                        color: '#3498db'
                    },
                    areaStyle: {
                        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                            { offset: 0, color: 'rgba(52, 152, 219, 0.8)' },
                            { offset: 1, color: 'rgba(52, 152, 219, 0.2)' }
                        ])
                    }
                }]
            };
            
            console.log('趋势图配置项设置完成');
            
            // 渲染图表
            console.log('开始设置echarts选项');
            myChart.setOption(option);
            console.log('趋势图渲染成功');
            
            // 响应窗口大小变化
            window.addEventListener('resize', function() {
                console.log('窗口大小改变，重设图表大小');
                myChart.resize();
            });
        } catch (error) {
            console.error('趋势图渲染失败:', error);
            analysis_showError(analysis_trendChartContainer, '渲染趋势图表失败: ' + error.message);
        }
    }
}

/**
 * 渲染默认的趋势图（模拟数据）
 */
function analysis_renderDefaultTrendChart() {
    console.log('渲染默认趋势图');
    
    // 模拟数据
    const mockTrendData = {
        keyword: ANALYSIS_DEFAULT_KEYWORD,
        timeUnit: 'day',
        trendData: [
            { timePoint: '2023-06-01', count: 5 },
            { timePoint: '2023-06-02', count: 8 },
            { timePoint: '2023-06-03', count: 12 },
            { timePoint: '2023-06-04', count: 6 },
            { timePoint: '2023-06-05', count: 15 },
            { timePoint: '2023-06-06', count: 9 },
            { timePoint: '2023-06-07', count: 11 }
        ]
    };
    
    // 渲染趋势图
    analysis_renderTrendChart(mockTrendData);
}

/**
 * 加载来源分布数据
 */
async function analysis_loadSourceDistribution() {
    if (!analysis_currentHistoryId) return;
    
    analysis_showLoading(analysis_sourceChartContainer);
    
    try {
        // 调试输出
        console.log('请求来源分布数据，historyId:', analysis_currentHistoryId);
        
        const requestBody = { historyId: analysis_currentHistoryId };
        console.log('请求体:', JSON.stringify(requestBody));
        
        const response = await fetch('/api/analysis/source-distribution', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestBody)
        });
        
        console.log('响应状态:', response.status);
        
        if (response.ok) {
            const data = await response.json();
            console.log('响应数据:', data);
            
            // 处理不同的响应结构
            if (data.sourceDistribution) {
                analysis_sourceDistributionData = data.sourceDistribution;
            } else if (data.sources) {
                analysis_sourceDistributionData = data.sources;
            } else if (Array.isArray(data)) {
                analysis_sourceDistributionData = data;
            } else {
                // 如果返回的是直接的对象结构 {source1: count1, source2: count2}
                analysis_sourceDistributionData = Object.entries(data).map(([source, count]) => ({
                    source: source,
                    count: count
                }));
            }
            
            console.log('提取的来源分布数据:', analysis_sourceDistributionData);
            
            if (analysis_sourceDistributionData && 
                (Array.isArray(analysis_sourceDistributionData) && analysis_sourceDistributionData.length > 0) || 
                (!Array.isArray(analysis_sourceDistributionData) && Object.keys(analysis_sourceDistributionData).length > 0)) {
                analysis_renderSourceDistribution();
            } else {
                console.log('来源分布数据为空，尝试从历史新闻数据生成');
                await analysis_generateSourceDistributionFromNews();
            }
        } else {
            const error = await response.text();
            console.error('加载来源分布数据失败:', error);
            console.log('尝试从历史新闻数据生成来源分布');
            await analysis_generateSourceDistributionFromNews();
        }
    } catch (error) {
        console.error('加载来源分布错误:', error);
        console.log('尝试从历史新闻数据生成来源分布');
        await analysis_generateSourceDistributionFromNews();
    }
}

/**
 * 从历史记录关联的新闻数据中生成来源分布
 */
async function analysis_generateSourceDistributionFromNews() {
    try {
        console.log('从历史ID获取新闻数据:', analysis_currentHistoryId);
        
        // 获取与历史记录关联的新闻数据
        const response = await fetch(`/api/history/${analysis_currentHistoryId}/news`);
        
        if (response.ok) {
            const newsData = await response.json();
            console.log('获取到的新闻数据:', newsData);
            
            if (Array.isArray(newsData) && newsData.length > 0) {
                // 统计来源分布
                const sourceCount = {};
                
                newsData.forEach(news => {
                    if (news.source) {
                        sourceCount[news.source] = (sourceCount[news.source] || 0) + 1;
                    }
                });
                
                // 转换为API格式
                analysis_sourceDistributionData = Object.entries(sourceCount).map(([source, count]) => ({
                    source: source,
                    count: count
                }));
                
                console.log('生成的来源分布数据:', analysis_sourceDistributionData);
                
                if (analysis_sourceDistributionData.length > 0) {
                    analysis_renderSourceDistribution();
                    return;
                }
            }
        }
        
        // 如果以上都失败，使用模拟数据
        console.log('无法从新闻数据生成来源分布，使用模拟数据');
        analysis_sourceDistributionData = [
            { source: "新浪新闻", count: 42 },
            { source: "人民日报", count: 28 },
            { source: "央视网", count: 16 },
            { source: "环球时报", count: 9 },
            { source: "中国日报", count: 5 }
        ];
        analysis_renderSourceDistribution();
        
    } catch (error) {
        console.error('从新闻数据生成来源分布错误:', error);
        analysis_showError(analysis_sourceChartContainer, '加载来源分布数据时发生错误');
    }
}

/**
 * 渲染来源分布图
 */
function analysis_renderSourceDistribution() {
    // 确保DOM元素存在
    if (!analysis_sourceChartContainer) {
        analysis_sourceChartContainer = document.getElementById('sourceChartContainer');
        if (!analysis_sourceChartContainer) {
            console.error('无法找到sourceChartContainer元素');
            return;
        }
    }

    if (!analysis_sourceDistributionData || analysis_sourceDistributionData.length === 0) {
        console.log('渲染来源分布: 数据为空');
        analysis_showEmptyState(analysis_sourceChartContainer, '没有足够的数据生成来源分布');
        return;
    }
    
    console.log('开始渲染来源分布图，数据:', analysis_sourceDistributionData);
    
    // 准备数据 - 处理可能的不同数据格式
    let chartData;
    
    // 如果数据已经是正确的格式 [{source: "来源", count: 数量}]
    if (analysis_sourceDistributionData[0] && 'source' in analysis_sourceDistributionData[0] && 'count' in analysis_sourceDistributionData[0]) {
        chartData = analysis_sourceDistributionData.map(item => ({
            name: item.source,
            value: item.count
        }));
    } 
    // 如果数据是 {来源: 数量} 格式
    else if (typeof analysis_sourceDistributionData === 'object' && !Array.isArray(analysis_sourceDistributionData)) {
        chartData = Object.entries(analysis_sourceDistributionData).map(([source, count]) => ({
            name: source,
            value: count
        }));
    }
    // 兜底，尝试直接使用数据
    else {
        chartData = analysis_sourceDistributionData;
    }
    
    console.log('处理后的图表数据:', chartData);
    
    if (!chartData || chartData.length === 0) {
        analysis_showEmptyState(analysis_sourceChartContainer, '数据格式不正确，无法生成来源分布');
        return;
    }
    
    // 验证每个数据项都有name和value字段
    const validChartData = chartData.filter(item => 
        item && typeof item === 'object' && 'name' in item && 'value' in item);
    
    if (validChartData.length === 0) {
        console.error('没有有效的图表数据项', chartData);
        analysis_showEmptyState(analysis_sourceChartContainer, '数据格式不正确，无法生成来源分布');
        return;
    }
    
    if (validChartData.length < chartData.length) {
        console.warn(`有 ${chartData.length - validChartData.length} 个无效的数据项被过滤`);
    }
    
    // 对数据按value值降序排序
    validChartData.sort((a, b) => b.value - a.value);
    
    // 只保留前三名数据用于饼图
    const top3Data = validChartData.slice(0, 3);
    console.log('前三大来源:', top3Data);
    
    // 清除容器内容（包括加载动画）
    analysis_sourceChartContainer.innerHTML = '';
    
    // 初始化echarts
    const myChart = echarts.init(analysis_sourceChartContainer, null, { renderer: 'svg' });
    
    // 配置项
    const option = {
        title: {
            text: '新闻主要来源分布(前三)',
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
            },
            data: top3Data.map(item => item.name)
        },
        series: [
            {
                name: '来源分布',
                type: 'pie',
                radius: '60%',
                center: ['50%', '60%'],
                data: top3Data,
                label: {
                    color: getComputedStyle(document.documentElement).getPropertyValue('--text-color').trim(),
                    formatter: '{b}: {c} ({d}%)'
                },
                emphasis: {
                    itemStyle: {
                        shadowBlur: 10,
                        shadowOffsetX: 0,
                        shadowColor: 'rgba(0, 0, 0, 0.5)'
                    }
                },
                color: ['#5470c6', '#91cc75', '#fac858']
            }
        ]
    };
    
    // 渲染图表
    try {
        myChart.setOption(option);
        console.log('饼图渲染成功');
        
    } catch (error) {
        console.error('饼图渲染失败:', error);
        analysis_showError(analysis_sourceChartContainer, '渲染来源分布图表失败');
    }
    
    // 响应窗口大小变化
    window.addEventListener('resize', function() {
        myChart.resize();
    });
}

/**
 * 显示加载中状态
 * @param {HTMLElement} container 容器元素
 */
function analysis_showLoading(container) {
    if (!container) return;
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
function analysis_showError(container, message) {
    if (!container) return;
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
function analysis_showEmptyState(container, message) {
    if (!container) return;
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

// 导出接口，提供给外部调用
window.dataAnalysis = {
    startAnalysis: function(historyId) {
        console.log('dataAnalysis.startAnalysis被调用，historyId =', historyId);
        analysis_startAnalysis(historyId);
    }
};

// 确认dataAnalysis对象已初始化
console.log('analysis.js已加载，dataAnalysis对象已初始化'); 