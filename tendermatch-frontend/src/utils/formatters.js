/**
 * Utility functions for human-readable specification formatting and AI reasoning text cleanup.
 */

/**
 * Returns a human-friendly operator label instead of raw programming symbols.
 * Examples:
 *   '==' -> ''
 *   '>=' -> 'Min '
 *   '<=' -> 'Max '
 *   'contains' -> 'Includes '
 */
export const formatOperatorPrefix = (operator) => {
  if (!operator) return '';
  const op = String(operator).trim().toLowerCase();

  switch (op) {
    case '==':
    case '=':
    case 'equals':
    case 'exact':
    case 'semantic':
      return '';
    case '>=':
    case 'gte':
    case 'min':
      return 'Min ';
    case '<=':
    case 'lte':
    case 'max':
      return 'Max ';
    case '>':
    case 'gt':
      return 'Over ';
    case '<':
    case 'lt':
      return 'Under ';
    case '!=':
    case '<>':
    case 'neq':
      return 'Not ';
    case 'contains':
    case 'includes':
      return 'Includes ';
    case 'range':
    case 'between':
      return 'Between ';
    default:
      return '';
  }
};

/**
 * Formats requirement criteria into natural human text without raw coding symbols.
 * Example:
 *   ('==', '13th Gen Intel Core i7', '') -> '13th Gen Intel Core i7'
 *   ('>=', '16', 'GB') -> 'Min 16 GB'
 *   ('<=', '1.5', 'kg') -> 'Max 1.5 kg'
 */
export const formatRequirementCriteria = (operator, value, unit) => {
  if (!value && value !== 0) return 'Specification requirement';

  let valStr = String(value).trim();
  // Strip any accidental leading symbols from raw value text like '== ' or '>= '
  valStr = valStr.replace(/^([=><!]{1,2})\s*/, '');

  const prefix = formatOperatorPrefix(operator);
  const unitStr = unit ? ` ${unit.trim()}` : '';

  // Avoid duplicate 'Min Min 16 GB' or 'Max Max 1.5 kg'
  const lowerVal = valStr.toLowerCase();
  if (prefix.toLowerCase().startsWith('min') && (lowerVal.startsWith('min') || lowerVal.startsWith('at least'))) {
    return `${valStr}${unitStr}`;
  }
  if (prefix.toLowerCase().startsWith('max') && (lowerVal.startsWith('max') || lowerVal.startsWith('up to'))) {
    return `${valStr}${unitStr}`;
  }

  return `${prefix}${valStr}${unitStr}`.trim();
};

/**
 * Transforms raw comparison traces, coding symbols, and developer strings in AI reasoning
 * into polished, professional, natural procurement language.
 */
export const formatAiReasoning = (rawReason, extraContext = {}) => {
  if (!rawReason || typeof rawReason !== 'string') {
    return 'Specification evaluated automatically via criteria engine.';
  }

  let text = rawReason.trim();

  // Pattern: Exact comparison: 'A' != 'B'
  const exactDiffMatch = text.match(/Exact comparison:\s*['"](.*?)['"]\s*!=\s*['"](.*?)['"]/i);
  if (exactDiffMatch) {
    const req = exactDiffMatch[1];
    const off = exactDiffMatch[2];
    return `Offered "${off}" differs from the required "${req}".`;
  }

  // Pattern: Exact comparison: 'A' == 'B'
  const exactSameMatch = text.match(/Exact comparison:\s*['"](.*?)['"]\s*==\s*['"](.*?)['"]/i);
  if (exactSameMatch) {
    const val = exactSameMatch[2] || exactSameMatch[1];
    return `Offered "${val}" matches the required criteria.`;
  }

  // Pattern: Substring containment: 'A' found in 'B'
  const containFoundMatch = text.match(/Substring containment:\s*['"](.*?)['"]\s*found in\s*['"](.*?)['"]/i);
  if (containFoundMatch) {
    return `Required term "${containFoundMatch[1]}" is present in the vendor specification.`;
  }

  // Pattern: Substring containment: 'A' not found in 'B'
  const containNotFoundMatch = text.match(/Substring containment:\s*['"](.*?)['"]\s*not found in\s*['"](.*?)['"]/i);
  if (containNotFoundMatch) {
    return `Required term "${containNotFoundMatch[1]}" was not found in the vendor proposal.`;
  }

  // Pattern: Offered X >= required Y
  const numGteMatch = text.match(/Offered\s+([\d\.]+.*?)\s*>=\s*required\s+([\d\.]+.*?)$/i);
  if (numGteMatch) {
    const isMatch = extraContext.verdict === 'Match' || extraContext.verdict === 'match';
    if (isMatch) {
      return `Offered ${numGteMatch[1]} satisfies the minimum requirement (Min ${numGteMatch[2]}).`;
    } else {
      return `Offered ${numGteMatch[1]} is below the required minimum (Min ${numGteMatch[2]}).`;
    }
  }

  // Pattern: Offered X <= required Y
  const numLteMatch = text.match(/Offered\s+([\d\.]+.*?)\s*<=\s*required\s+([\d\.]+.*?)$/i);
  if (numLteMatch) {
    const isMatch = extraContext.verdict === 'Match' || extraContext.verdict === 'match';
    if (isMatch) {
      return `Offered ${numLteMatch[1]} meets the maximum limit (Max ${numLteMatch[2]}).`;
    } else {
      return `Offered ${numLteMatch[1]} exceeds the maximum allowed limit (Max ${numLteMatch[2]}).`;
    }
  }

  // Pattern: Offered X == required Y
  const numEqMatch = text.match(/Offered\s+([\d\.]+.*?)\s*==\s*required\s+([\d\.]+.*?)$/i);
  if (numEqMatch) {
    return `Offered ${numEqMatch[1]} matches required target ${numEqMatch[2]}.`;
  }

  // Pattern: High cross-encoder semantic score: 0.92
  const highCeMatch = text.match(/High cross-encoder semantic score:\s*([\d\.]+)/i);
  if (highCeMatch) {
    const pct = Math.round(parseFloat(highCeMatch[1]) * 100);
    return `Strong semantic match (${pct}% confidence alignment with requirement).`;
  }

  // Pattern: Low cross-encoder semantic score: 0.15
  const lowCeMatch = text.match(/Low cross-encoder semantic score:\s*([\d\.]+)/i);
  if (lowCeMatch) {
    const pct = Math.round(parseFloat(lowCeMatch[1]) * 100);
    return `Low semantic match (${pct}% alignment with requirement).`;
  }

  // Cleanup any lingering code artifacts
  text = text
    .replace(/\s*!=\s*/g, ' does not match ')
    .replace(/\s*==\s*/g, ' matches ')
    .replace(/\s*>=\s*/g, ' meets min ')
    .replace(/\s*<=\s*/g, ' within max ')
    .replace(/Requirement omitted \/ not offered by vendor/i, 'Specification omitted or not offered in vendor proposal.');

  return text;
};
