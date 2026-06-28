/**
 * Minimal CLI argument parser.
 * Parses --key value or --key=value pairs.
 *
 * @param {string[]} argv
 * @returns {Record<string, string>}
 */
export function parseArgs(argv) {
  const result = {};
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg.startsWith('--')) {
      const [key, maybeVal] = arg.slice(2).split('=');
      if (maybeVal !== undefined) {
        result[key] = maybeVal;
      } else if (argv[i + 1] && !argv[i + 1].startsWith('--')) {
        result[key] = argv[++i];
      } else {
        result[key] = 'true';
      }
    }
  }
  return result;
}
