function parseArgs(argv) {
  const result = {};
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg.startsWith('--')) {
      const [key, val] = arg.slice(2).split('=');
      if (val !== undefined) result[key] = val;
      else if (argv[i + 1] && !argv[i + 1].startsWith('--')) result[key] = argv[++i];
      else result[key] = 'true';
    }
  }
  return result;
}
module.exports = { parseArgs };
