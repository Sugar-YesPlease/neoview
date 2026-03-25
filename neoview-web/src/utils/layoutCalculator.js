/**
 * 布局计算工具模块
 * 用于根据参与者数量和容器宽高比计算最优网格布局
 * 参考 Google Meet 风格：统一正方形卡片，紧凑间距
 */

// 卡片间距 - 紧凑 6px
const GAP = 6;

// 九宫格极小留白 0.5%
const PADDING_RATIO = 0.005;

// 视频比例常量
// aspectRatio = width / height
const ASPECT_RATIO = {
  SQUARE: 1,        // 1:1 正方形
  HORIZONTAL: 9/16, // 高>宽（竖向长条）
  VERTICAL: 16/9,   // 宽>高（横向）
};

/**
 * 计算可用空间（减去留白）
 * @param {number} containerWidth - 容器宽度
 * @param {number} containerHeight - 容器高度
 * @returns {{ availableWidth: number, availableHeight: number }}
 */
function calculateAvailableSpace(containerWidth, containerHeight) {
  return {
    availableWidth: containerWidth * (1 - PADDING_RATIO * 2),
    availableHeight: containerHeight * (1 - PADDING_RATIO * 2),
  };
}

/**
 * 计算正方形布局的卡片尺寸
 * @param {number} availableWidth - 可用宽度
 * @param {number} availableHeight - 可用高度
 * @param {number} cols - 列数（网格容量）
 * @param {number} rows - 行数（网格容量）
 * @param {number} count - 实际参与者数量
 * @returns {{ cardWidth: number, cardHeight: number }}
 */
function calculateSquareCardSize(availableWidth, availableHeight, cols, rows, count) {
  // 按实际参与者数量计算，而不是按网格容量
  // 这样 3人用2x2网格时，卡片会按3人实际占用的空间来算
  
  const totalGapWidth = (cols - 1) * GAP;
  const totalGapHeight = (rows - 1) * GAP;

  // 按列数计算每个卡片的宽度
  const maxCardWidth = (availableWidth - totalGapWidth) / cols;
  // 按行数计算每个卡片的高度
  const maxCardHeight = (availableHeight - totalGapHeight) / rows;

  // 正方形，取较小值
  const cardSize = Math.min(maxCardWidth, maxCardHeight);

  return {
    cardWidth: cardSize,
    cardHeight: cardSize,
  };
}

/**
 * 根据参与者数量获取行列配置和卡片比例
 * @param {number} count - 参与者数量
 * @returns {{ cols: number, rows: number, aspectRatio: number }}
 */
function getGridConfig(count) {
  // 1-2人：1行，正方形
  // 3-5人：1行，竖向长条（高>宽）
  // 6-9人：固定3x3九宫格，正方形卡片填满
  
  if (count === 1) {
    return { cols: 1, rows: 1, aspectRatio: ASPECT_RATIO.SQUARE };
  } else if (count === 2) {
    return { cols: 2, rows: 1, aspectRatio: ASPECT_RATIO.SQUARE };
  } else if (count >= 3 && count <= 5) {
    return { cols: count, rows: 1, aspectRatio: ASPECT_RATIO.HORIZONTAL };
  } else if (count >= 6 && count <= 9) {
    // 固定3x3九宫格，正方形卡片
    return { cols: 3, rows: 3, aspectRatio: ASPECT_RATIO.SQUARE };
  } else {
    // 10人以上：固定3x3九宫格
    return { cols: 3, rows: 3, aspectRatio: ASPECT_RATIO.SQUARE };
  }
}

/**
 * 10人以上的动态布局计算
 * @param {number} count - 参与者数量
 * @param {boolean} isLandscape - 是否为横屏
 * @returns {{ cols: number, rows: number }}
 */
function calculateDynamicGrid(count, isLandscape) {
  // 尽量接近正方形
  const cols = isLandscape
    ? Math.ceil(Math.sqrt(count * 1.3))  // 横屏多列
    : Math.ceil(Math.sqrt(count));       // 竖屏少列

  const rows = Math.ceil(count / cols);

  // 限制最大行列数
  return {
    cols: Math.min(cols, 6),
    rows: Math.min(rows, 6),
  };
}

/**
 * 根据行配置生成每行的参与者索引数组
 * @param {number} count - 参与者数量
 * @param {number} cols - 列数
 * @param {number} rows - 行数
 * @returns {Array<Array<number>>}
 */
function generateRowsData(count, cols, rows) {
  const rowsData = [];
  let index = 0;

  for (let r = 0; r < rows; r++) {
    const row = [];
    for (let c = 0; c < cols; c++) {
      if (index < count) {
        row.push(index++);
      } else {
        row.push(-1); // 空位置用 -1 表示
      }
    }
    rowsData.push(row);
  }

  return rowsData;
}

/**
 * 根据参与者数量和容器宽高比计算最优网格布局
 * @param {number} participantCount - 参与者总数
 * @param {number} containerWidth - 容器宽度
 * @param {number} containerHeight - 容器高度
 * @returns {{ rows: number, cols: number, rowConfig: number[], rowsData: Array, cardSizes: Array, style: object }}
 */
export function calculateGridLayout(participantCount, containerWidth, containerHeight) {
  // 参数校验
  if (participantCount <= 0) {
    return createEmptyLayout();
  }

  if (!containerWidth || !containerHeight || containerWidth <= 0 || containerHeight <= 0) {
    return createEmptyLayout();
  }

  // 获取行列配置和卡片比例
  const { cols, rows, aspectRatio } = getGridConfig(participantCount);

  // 计算可用空间
  const { availableWidth, availableHeight } = calculateAvailableSpace(containerWidth, containerHeight);

  // 生成每行的参与者索引
  const rowsData = generateRowsData(participantCount, cols, rows);
  const rowConfig = rowsData.map(row => row.length);

  // 计算卡片尺寸 - 根据卡片比例
  let cardWidth, cardHeight;
  
  if (rows === 1) {
    // 单行布局：根据宽度计算
    const totalColGap = (participantCount - 1) * GAP;
    const availableCardWidth = (availableWidth - totalColGap) / participantCount;
    cardWidth = availableCardWidth;
    cardHeight = cardWidth / aspectRatio;
    
    // 检查高度是否超出
    if (cardHeight > availableHeight) {
      cardHeight = availableHeight;
      cardWidth = cardHeight * aspectRatio;
    }
  } else {
    // 多行布局（九宫格）- 固定填满整个网格
    const totalColGap = (cols - 1) * GAP;
    const totalRowGap = (rows - 1) * GAP;
    
    // 直接按网格分配空间，不考虑比例限制
    cardWidth = (availableWidth - totalColGap) / cols;
    cardHeight = (availableHeight - totalRowGap) / rows;
  }

  // 生成每个卡片的尺寸
  const cardSizes = [];
  for (let i = 0; i < participantCount; i++) {
    cardSizes.push({ width: cardWidth, height: cardHeight });
  }

  return {
    cols,
    rows,
    rowConfig,
    rowsData,
    cardSizes,
    isGridMode: rows > 1, // 九宫格模式标志
    style: {
      display: 'flex',
      flexDirection: rows > 1 ? 'column' : 'row',
      justifyContent: 'center',
      alignItems: 'center',
      width: '100%',
      height: '100%',
      gap: `${GAP}px`,
      padding: `${containerHeight * PADDING_RATIO}px ${containerWidth * PADDING_RATIO}px`,
    },
    rowStyle: {
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      gap: `${GAP}px`,
    },
  };
}

/**
 * 创建空布局
 */
function createEmptyLayout() {
  return {
    cols: 1,
    rows: 1,
    rowConfig: [1],
    rowsData: [[0]],
    cardSizes: [{ width: 100, height: 100 }],
    style: {},
    rowStyle: {},
  };
}

/**
 * 获取参与者在网格中的位置（保留接口）
 * @param {number} index - 参与者索引
 * @param {number} cols - 列数
 * @returns {{ gridArea: string }}
 */
export function getGridPosition(index, cols) {
  return {
    gridArea: `cell${index}`,
  };
}
