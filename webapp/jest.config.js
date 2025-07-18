export default {
  testEnvironment: 'node',
  transform: {
    '^.+\\.[jt]sx?$': ['babel-jest', { configFile: './.babelrc' }],
  },
  moduleNameMapper: {},
  testPathIgnorePatterns: ['/node/']
};
