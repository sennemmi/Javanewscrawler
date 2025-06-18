 /**
 * 数据分析页面脚本
 */
$(document).ready(function() {
    // 检查用户登录状态
    checkLoginStatus();

    // 初始化时间范围（默认过去30天）
    initDateRange();

    // 初始化所有图表
    initAllCharts();

    // 事件绑定
    bindEvents();
});

/**
 * 检查用户登录状态
 */
function checkLoginStatus() {
    $.ajax({
        url: '/api/user/current',
        type: 'GET',
        success: function(response) {
            if (response.username) {
                $('#username').text('欢迎, ' + response.username);
            } else {
                // 未登录，跳转到登录页
                window.location.href = 'login.html';
            }
        },
        error: function() {
            // 获取用户信息失败，跳转到登录页
            window.location.href = 'login.html';
        }
    });
}

/**
 * 初始化时间范围（默认过去30天）
 */
function initDateRange() {
    const now = new Date();
    const endDate = now.toISOString().slice(0, 16);
    
    // 30天前
    const startDate = new Date(now);
    startDate.setDate(now.getDate() - 30);
    
    $('#endDate').val(endDate);
    $('#startDate').val(startDate.toISOString().slice(0, 16));
}

/**
 * 初始化所有图表
 */
function initAllCharts() {
    // 初始化各个图表
    loadWordCloud();
    loadHotWords();
    loadSourceDistribution();
    
    // 时间趋势图需要关键词，默认加载一个热门关键词
    loadInitialTrendKeyword();
}

/**
 * 绑定事件处理
 */
function bindEvents() {
    // 应用日期过滤器
    $('#apply-date-filter').on('click', function() {
        refreshAllCharts();
    });
    
    // 词云数据源切换
    $('.word-cloud-source').on('click', function(e) {
        e.preventDefault();
        const source = $(this).data('source');
        $('#wordCloudSourceDropdown').text('数据源: ' + getSourceName(source));
        loadWordCloud(source);
    });
    
    // 时间趋势查询
    $('#search-trend').on('click', function() {
        const keyword = $('#trend-keyword').val().trim();
        if (keyword) {
            loadTimeTrend(keyword);
        } else {
            alert('请输入关键词');
        }
    });
    
    // 退出登录
    $('#logout-btn').on('click', function() {
        $.ajax({
            url: '/api/user/logout',
            type: 'POST',
            success: function() {
                window.location.href = 'login.html';
            }
        });
    });
}

/**
 * 刷新所有图表
 */
function refreshAllCharts() {
    loadWordCloud($('#wordCloudSourceDropdown').text().includes('标题') ? 'title' : 
                 $('#wordCloudSourceDropdown').text().includes('正文') ? 'content' : 'keywords');
    loadHotWords();
    loadSourceDistribution();
    
    const keyword = $('#trend-keyword').val().trim();
    if (keyword) {
        loadTimeTrend(keyword);
    }
}

/**
 * 获取数据源名称
 */
function getSourceName(source) {
    switch(source) {
        case 'title': return '标题';
        case 'content': return '正文';
        case 'keywords': return '关键词';
        default: return '标题';
    }
}

/**
 * 获取当前时间范围参数
 */
function getTimeRangeParams() {
    const startDate = $('#startDate').val() ? new Date($('#startDate').val()).toISOString() : null;
    const endDate = $('#endDate').val() ? new Date($('#endDate').val()).toISOString() : null;
    
    return {
        startDate: startDate,
        endDate: endDate
    };
}

/**
 * 加载词云数据并渲染
 */
function loadWordCloud(source = 'title') {
    // 显示加载中，隐藏空数据提示
    $('#word-cloud-loading').show();
    $('#word-cloud-empty').addClass('d-none');
    $('#word-cloud-container').html('');
    
    // 准备请求参数
    const params = {
        source: source,
        limit: 100,
        ...getTimeRangeParams()
    };
    
    // 发送请求
    $.ajax({
        url: '/api/analysis/word-cloud',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(params),
        success: function(response) {
            $('#word-cloud-loading').hide();
            
            if (response.wordCloud && response.wordCloud.length > 0) {
                renderWordCloud(response.wordCloud);
            } else {
                $('#word-cloud-empty').removeClass('d-none');
            }
        },
        error: function(xhr) {
            $('#word-cloud-loading').hide();
            $('#word-cloud-empty').removeClass('d-none');
            console.error('加载词云数据失败:', xhr.responseText);
        }
    });
}

/**
 * 渲染词云
 */
function renderWordCloud(wordCloudData) {
    // 转换数据格式为wordcloud2.js所需的格式
    const list = wordCloudData.map(item => [item.text, item.weight]);
    
    // 渲染词云
    WordCloud(document.getElementById('word-cloud-container'), { 
        list: list,
        gridSize: 8,
        weightFactor: 1,
        fontFamily: 'Microsoft YaHei, sans-serif',
        color: function() {
            return 'hsl(' + Math.floor(Math.random() * 360) + ', 70%, 50%)';
        },
        hover: function(item) {
            if (item) {
                console.log(item[0] + ': ' + item[1]);
            }
        },
        click: function(item) {
            if (item) {
                $('#trend-keyword').val(item[0]);
                loadTimeTrend(item[0]);
            }
        },
        classes: 'word-cloud-text'
    });
}

/**
 * 加载热词排行榜数据并渲染
 */
function loadHotWords() {
    // 显示加载中，隐藏空数据提示
    $('#hot-words-loading').show();
    $('#hot-words-empty').addClass('d-none');
    
    // 准备请求参数
    const params = {
        limit: 20,
        ...getTimeRangeParams()
    };
    
    // 发送请求
    $.ajax({
        url: '/api/analysis/hot-words',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(params),
        success: function(response) {
            $('#hot-words-loading').hide();
            
            if (response.hotWords && response.hotWords.length > 0) {
                renderHotWords(response.hotWords);
            } else {
                $('#hot-words-empty').removeClass('d-none');
            }
        },
        error: function(xhr) {
            $('#hot-words-loading').hide();
            $('#hot-words-empty').removeClass('d-none');
            console.error('加载热词数据失败:', xhr.responseText);
        }
    });
}

/**
 * 渲染热词排行榜
 */
function renderHotWords(hotWords) {
    // 初始化ECharts实例
    const chart = echarts.init(document.getElementById('hot-words-chart'));
    
    // 数据预处理
    const data = hotWords.map(item => ({
        name: item.word,
        value: item.count
    }));
    
    // 图表配置
    const option = {
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'shadow'
            }
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
        },
        xAxis: {
            type: 'value',
            boundaryGap: [0, 0.01]
        },
        yAxis: {
            type: 'category',
            data: data.map(item => item.name),
            axisLabel: {
                interval: 0,
                rotate: 0
            }
        },
        series: [
            {
                name: '出现次数',
                type: 'bar',
                data: data.map(item => item.value),
                itemStyle: {
                    color: function(params) {
                        const colorList = [
                            '#c23531', '#2f4554', '#61a0a8', '#d48265', '#91c7ae',
                            '#749f83', '#ca8622', '#bda29a', '#6e7074', '#546570'
                        ];
                        return colorList[params.dataIndex % colorList.length];
                    }
                }
            }
        ]
    };
    
    // 设置图表
    chart.setOption(option);
    
    // 窗口大小改变时，调整图表大小
    window.addEventListener('resize', function() {
        chart.resize();
    });
}

/**
 * 初始化时间趋势分析的关键词
 */
function loadInitialTrendKeyword() {
    // 先尝试获取热词，用第一个热词作为默认关键词
    $.ajax({
        url: '/api/analysis/hot-words',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            limit: 1,
            ...getTimeRangeParams()
        }),
        success: function(response) {
            let keyword = '科技'; // 默认关键词
            
            if (response.hotWords && response.hotWords.length > 0) {
                keyword = response.hotWords[0].word;
            }
            
            $('#trend-keyword').val(keyword);
            loadTimeTrend(keyword);
        },
        error: function() {
            // 获取热词失败，使用默认关键词
            const keyword = '科技';
            $('#trend-keyword').val(keyword);
            loadTimeTrend(keyword);
        }
    });
}

/**
 * 加载时间趋势数据并渲染
 */
function loadTimeTrend(keyword) {
    // 显示加载中，隐藏空数据提示
    $('#time-trend-loading').show();
    $('#time-trend-empty').addClass('d-none');
    
    // 准备请求参数
    const params = {
        keyword: keyword,
        timeUnit: $('#time-unit').val(),
        ...getTimeRangeParams()
    };
    
    // 发送请求
    $.ajax({
        url: '/api/analysis/time-trend',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(params),
        success: function(response) {
            $('#time-trend-loading').hide();
            
            if (response.trendData && response.trendData.length > 0) {
                renderTimeTrend(response.trendData, keyword);
            } else {
                $('#time-trend-empty').removeClass('d-none');
            }
        },
        error: function(xhr) {
            $('#time-trend-loading').hide();
            $('#time-trend-empty').removeClass('d-none');
            console.error('加载时间趋势数据失败:', xhr.responseText);
        }
    });
}

/**
 * 渲染时间趋势图
 */
function renderTimeTrend(trendData, keyword) {
    // 初始化ECharts实例
    const chart = echarts.init(document.getElementById('time-trend-chart'));
    
    // 数据预处理
    const xAxisData = trendData.map(item => item.timePoint);
    const seriesData = trendData.map(item => item.count);
    
    // 图表配置
    const option = {
        title: {
            text: `关键词"${keyword}"的时间趋势`,
            textStyle: {
                fontSize: 14
            }
        },
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'shadow'
            }
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
        },
        xAxis: {
            type: 'category',
            data: xAxisData,
            axisLabel: {
                interval: 0,
                rotate: 30
            }
        },
        yAxis: {
            type: 'value'
        },
        series: [
            {
                name: '出现次数',
                type: 'line',
                data: seriesData,
                markPoint: {
                    data: [
                        {type: 'max', name: '最大值'},
                        {type: 'min', name: '最小值'}
                    ]
                },
                markLine: {
                    data: [
                        {type: 'average', name: '平均值'}
                    ]
                },
                smooth: true,
                lineStyle: {
                    width: 3,
                    color: '#5470c6'
                },
                areaStyle: {
                    opacity: 0.3,
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                        {offset: 0, color: 'rgba(84, 112, 198, 0.5)'},
                        {offset: 1, color: 'rgba(84, 112, 198, 0.1)'}
                    ])
                }
            }
        ]
    };
    
    // 设置图表
    chart.setOption(option);
    
    // 窗口大小改变时，调整图表大小
    window.addEventListener('resize', function() {
        chart.resize();
    });
}

/**
 * 加载来源分布数据并渲染
 */
function loadSourceDistribution() {
    // 显示加载中，隐藏空数据提示
    $('#source-loading').show();
    $('#source-empty').addClass('d-none');
    
    // 准备请求参数
    const params = {
        ...getTimeRangeParams()
    };
    
    // 发送请求
    $.ajax({
        url: '/api/analysis/source-distribution',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(params),
        success: function(response) {
            $('#source-loading').hide();
            
            if (response.sourceDistribution && response.sourceDistribution.length > 0) {
                renderSourceDistribution(response.sourceDistribution);
            } else {
                $('#source-empty').removeClass('d-none');
            }
        },
        error: function(xhr) {
            $('#source-loading').hide();
            $('#source-empty').removeClass('d-none');
            console.error('加载来源分布数据失败:', xhr.responseText);
        }
    });
}

/**
 * 渲染来源分布图
 */
function renderSourceDistribution(sourceData) {
    // 初始化ECharts实例
    const chart = echarts.init(document.getElementById('source-distribution-chart'));
    
    // 数据预处理
    const data = sourceData.map(item => ({
        name: item.source,
        value: item.count
    }));
    
    // 图表配置
    const option = {
        tooltip: {
            trigger: 'item',
            formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
            orient: 'vertical',
            right: 10,
            top: 'center',
            type: 'scroll',
            formatter: function(name) {
                return name.length > 10 ? name.substring(0, 10) + '...' : name;
            }
        },
        series: [
            {
                name: '来源分布',
                type: 'pie',
                radius: ['40%', '70%'],
                avoidLabelOverlap: false,
                itemStyle: {
                    borderRadius: 10,
                    borderColor: '#fff',
                    borderWidth: 2
                },
                label: {
                    show: false,
                    position: 'center'
                },
                emphasis: {
                    label: {
                        show: true,
                        fontSize: '16',
                        fontWeight: 'bold'
                    }
                },
                labelLine: {
                    show: false
                },
                data: data
            }
        ]
    };
    
    // 设置图表
    chart.setOption(option);
    
    // 窗口大小改变时，调整图表大小
    window.addEventListener('resize', function() {
        chart.resize();
    });
}